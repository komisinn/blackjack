// G6 ゲームプレイ画面の動き（Ajax）
//
// 流れ：
//   1. 画面を開いたら GET /game/api/state で今の状態（JSON）をもらって描く
//   2. ボタンを押したら POST /game/api/action で行動を送り、返ってきた状態で描き直す
//   3. 決着したら少し待って G7（結果）へ移動する
//
// カードを配るのも勝敗を決めるのもサーバー側。ここは「もらった状態を描くだけ」。

const base = document.body.dataset.base; // アプリのURLの先頭（例："/"）

// POSTする時に必要なCSRFトークン（<head> の meta タグに入っている）
const csrfToken = document.querySelector('meta[name="_csrf"]').content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;

const actionButtons = document.querySelectorAll('.act');

// ---------- 演出の速さ（ここの数字を変えると速さが変わる） ----------
const CARD_DELAY = 650;    // カードを1枚出すごとに待つ時間（ミリ秒。1000 = 1秒）
const RESULT_DELAY = 2500; // 決着してから結果画面（G7）へ移るまでの時間

let shown = null;          // 今画面に出しているカードの枚数（席ごと・ディーラー）
let animating = false;     // 演出の最中は true（ボタンを押せなくする）
const errorMsg = document.getElementById('errorMsg');

// ---------- 描画 ----------

// 文字を安全にHTMLへ入れるための変換（< や & をそのまま出さない）
function esc(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// カード1枚のHTML
function cardHtml(card) {
    if (card.hidden) {
        return '<div class="pc back">?</div>'; // 裏向き
    }
    const red = card.red ? 'red' : '';
    return `<div class="pc ${red}"><span class="r">${esc(card.rank)}</span><span class="s">${esc(card.suit)}</span></div>`;
}

// コイン付きの数字
function coinHtml(value) {
    return `<span class="num"><span class="coin">c</span>${value.toLocaleString()}</span>`;
}

// 賭け金に合う色のチップを選ぶ（100なら黒チップ、など）
function chipClass(bet) {
    for (const v of [500, 250, 100, 50, 25, 10, 5]) {
        if (bet >= v) {
            return 'c' + v;
        }
    }
    return 'c5';
}

// ---------- 描き方 ----------
// 毎回テーブル全体を作り直すと、全員のカードが一瞬消えて「びくっ」と動いてしまう。
// そこで、席の入れ物は最初に1回だけ作り、そのあとは変わったところ（増えたカード、合計、タグ）だけ書き換える。

// 1つの手札（cardbox）を、cards の内容に合わせる
//   ・すでに出ているカードはそのまま（動かさない）
//   ・裏向きだったカードがめくれたら、その1枚だけ入れ替える
//   ・増えたカードは後ろに足す（足したカードだけ、ふわっと出る）
function syncCards(box, cards) {
    cards.forEach((card, i) => {
        const html = cardHtml(card);
        const old = box.children[i];
        if (!old) {
            box.insertAdjacentHTML('beforeend', html);
            box.lastElementChild.classList.add('deal-in');
        } else if (old.dataset.key !== cardKey(card)) {
            old.outerHTML = html;
            box.children[i].classList.add('deal-in');
        }
        box.children[i].dataset.key = cardKey(card);
    });
    // カードが減った場合（新しいゲームなど）は余りを消す
    while (box.children.length > cards.length) {
        box.lastElementChild.remove();
    }
}

// カードを見分けるための文字（裏向きなら "?"）
function cardKey(card) {
    return card.hidden ? '?' : card.rank + card.suit;
}

// 席の入れ物を作る（席の数が変わった時だけ）
function buildSeats(view) {
    const seatsEl = document.getElementById('seats');
    if (seatsEl.children.length === view.seats.length) {
        return; // もう作ってある
    }
    seatsEl.innerHTML = view.seats.map((seat, i) => {
        const youClass = seat.you ? 'you' : '';
        const betChip = seat.you ? `<div class="betchip" id="betchip"></div>` : '';
        return `<div>
            ${betChip}
            <span class="namepill ${youClass}">${esc(seat.name)}</span>
            <div class="seatchips"><span id="seatChips${i}"></span> <span class="tag" id="seatTag${i}"></span></div>
            <div class="cardbox" id="seatCards${i}"></div>
            <div class="tot" id="seatTotal${i}"></div>
        </div>`;
    }).join('');
}

// サーバーからもらった状態（view）で画面を描く（変わったところだけ書き換える）
function render(view) {
    // ① ディーラー
    syncCards(document.getElementById('dealerCards'), view.dealer.cards);
    document.getElementById('dealerLabel').textContent = view.dealer.label;

    // ②③ 各席
    buildSeats(view);
    view.seats.forEach((seat, i) => {
        document.getElementById('seatChips' + i).innerHTML = coinHtml(seat.chips);
        const tag = document.getElementById('seatTag' + i);
        tag.textContent = seat.tag;
        tag.className = 'tag ' + seat.tagClass;
        syncCards(document.getElementById('seatCards' + i), seat.cards);
        document.getElementById('seatTotal' + i).textContent = (seat.you ? '合計 ' : '') + seat.total;
        if (seat.you) {
            const chip = document.getElementById('betchip');
            chip.className = 'betchip ' + chipClass(seat.bet);
            chip.textContent = seat.bet;
        }
    });

    // ④ ベット額・所持チップ
    document.getElementById('betLine').textContent =
        `Bet ${view.bet.toLocaleString()}　／　Chips ${view.chips.toLocaleString()}`;

    // ⑨ 結果ログ
    document.getElementById('resLog').innerHTML = view.log.map(esc).join('<br>');

    // ⑤〜⑧ ボタンを押せるかどうか（演出の最中はすべて押せない）
    actionButtons.forEach(btn => {
        const action = btn.dataset.action;
        let enabled = !view.finished && !animating;
        if (action === 'DOUBLE') enabled = enabled && view.canDouble;
        if (action === 'SURRENDER') enabled = enabled && view.canSurrender;
        btn.disabled = !enabled;
    });
}

// ---------- ゆっくり配る演出 ----------

// 待つ（例：await wait(500) で0.5秒止まる）
function wait(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

// 見えているカードだけで合計を計算する（演出の途中で使う）
//   A は 11 として数え、21 を超えたら 1 に直す
function calcTotal(cards) {
    let total = 0;
    let aces = 0;
    for (const c of cards) {
        if (c.hidden) continue;
        if (c.rank === 'A') { total += 11; aces++; }
        else if (['J', 'Q', 'K'].includes(c.rank)) total += 10;
        else total += Number(c.rank);
    }
    while (total > 21 && aces > 0) { total -= 10; aces--; }
    return total;
}

// 「各席・ディーラーのカードを count 枚まで見せた状態」の view を作る
function partialView(view, count, last) {
    const v = structuredClone(view);
    v.seats.forEach((seat, i) => {
        seat.cards = seat.cards.slice(0, count.seats[i]);
        if (!last) {
            seat.total = calcTotal(seat.cards);
            // AI のタグは、自分のカードが出るまで「待機中」のままにしておく（結果を先に見せない）
            if (!seat.you && view.finished) { seat.tag = '待機中'; seat.tagClass = 'wait'; }
        }
    });
    v.dealer.cards = v.dealer.cards.slice(0, count.dealer);
    if (!last && view.finished) {
        v.dealer.label = '合計 ' + calcTotal(v.dealer.cards);
    }
    return v;
}

// 前の状態（shown）から新しい状態（view）まで、カードを1枚ずつ出していく
async function animateTo(view) {
    animating = true;
    const target = { seats: view.seats.map(s => s.cards.length), dealer: view.dealer.cards.length };
    const count = shown
        ? structuredClone(shown)
        : { seats: view.seats.map(() => 0), dealer: 0 };

    // 1枚出すたびに描き直して、少し待つ
    async function step() {
        render(partialView(view, count, false));
        await wait(CARD_DELAY);
    }

    // 最初に開いたとき：本物のカジノと同じく、全員に1枚ずつ2周配る
    if (!shown) {
        for (let round = 1; round <= 2; round++) {
            for (let i = 0; i < count.seats.length; i++) {
                if (count.seats[i] < Math.min(round, target.seats[i])) { count.seats[i]++; await step(); }
            }
            if (count.dealer < Math.min(round, target.dealer)) { count.dealer++; await step(); }
        }
    }

    // 席の順番（AI1 → あなた → AI2 → AI3）に、増えたカードを1枚ずつ出す
    for (let i = 0; i < count.seats.length; i++) {
        while (count.seats[i] < target.seats[i]) { count.seats[i]++; await step(); }
    }

    // ディーラー：決着したら、まず裏向きのカードをめくり、そのあと引いたカードを1枚ずつ出す
    if (view.finished && shown && shown.dealerHidden) {
        await step(); // めくる（view では裏向きではなくなっている）
    }
    while (count.dealer < target.dealer) { count.dealer++; await step(); }

    // 最後に、サーバーからもらった本当の状態で描く
    animating = false;
    render(view);
    shown = { seats: target.seats, dealer: target.dealer,
              dealerHidden: view.dealer.cards.some(c => c.hidden) };

    // 決着したら、結果を少し見せてから G7 へ
    if (view.finished) {
        const label = view.resultLabel ? '結果：' + view.resultLabel + '　' : '';
        document.getElementById('footnote').textContent = label + 'まもなく結果画面へ移動します…';
        setTimeout(() => {
            location.href = base + 'game/result';
        }, RESULT_DELAY);
    }
}

// ---------- サーバーとのやりとり ----------

// 今の状態をもらう
async function loadState() {
    const response = await fetch(base + 'game/api/state');
    if (!response.ok) {
        location.href = base + 'game/bet'; // ゲームが無ければベット画面へ
        return;
    }
    animateTo(await response.json());
}

// 行動を送る
async function sendAction(action) {
    errorMsg.textContent = '';
    actionButtons.forEach(btn => btn.disabled = true); // 二重クリック防止

    const headers = { 'Content-Type': 'application/x-www-form-urlencoded' };
    headers[csrfHeader] = csrfToken;

    try {
        const response = await fetch(base + 'game/api/action', {
            method: 'POST',
            headers: headers,
            body: 'action=' + encodeURIComponent(action)
        });
        const data = await response.json();
        if (!response.ok) {
            errorMsg.textContent = data.error; // 「今はダブルできません」など
            loadState();                       // ボタンの状態を元に戻す
            return;
        }
        animateTo(data);
    } catch (e) {
        errorMsg.textContent = '通信に失敗しました。もう一度お試しください。';
        loadState();
    }
}

// ボタンにクリック処理を付ける
actionButtons.forEach(btn => {
    btn.addEventListener('click', () => sendAction(btn.dataset.action));
});

loadState(); // 画面を開いた時に最初の状態を描く
