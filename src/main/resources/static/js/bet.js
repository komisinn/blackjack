// G5 ベット画面の動き
// チップを押す → 賭け金に加算 / 積んだチップを押す → 1枚戻す / クリア / オールイン

// 画面の部品を取ってくる
const betForm = document.getElementById('betForm');
const maxChips = Number(betForm.dataset.max); // 所持チップ（HTMLの data-max から読む）
const chipButtons = document.querySelectorAll('#chips .chip');
const stackedArea = document.getElementById('stacked');
const betAmount = document.getElementById('betAmount');
const betInput = document.getElementById('betInput');
const dealBtn = document.getElementById('dealBtn');

// 置いたチップを順番に覚えておく配列（例：[50, 25, 25]）
let stack = [];

// 賭け金の合計を計算する
function total() {
    let sum = 0;
    for (const v of stack) {
        sum += v;
    }
    return sum;
}

// 画面を今の状態に合わせて描き直す
function render() {
    const bet = total();

    // ⑤ 賭け金
    betAmount.textContent = bet.toLocaleString();
    betInput.value = bet;

    // ④ 積まれたチップ
    stackedArea.innerHTML = '';
    if (stack.length === 0) {
        stackedArea.innerHTML = '<span class="small" style="color:var(--muted)">チップなし</span>';
    }
    stack.forEach((value, index) => {
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'chip c' + value;
        btn.textContent = value;
        btn.title = '1枚戻す';
        btn.addEventListener('click', () => {
            stack.splice(index, 1); // その1枚を取り除く
            render();
        });
        stackedArea.appendChild(btn);
    });

    // ③ 所持チップを超えるチップは押せないようにする
    chipButtons.forEach(btn => {
        btn.disabled = bet + Number(btn.dataset.chip) > maxChips;
    });

    // ⑦ 賭け金が1以上・所持チップ以下の時だけ「ベットする」を押せる
    dealBtn.disabled = bet < 1 || bet > maxChips;
}

// ③ チップを押した時
chipButtons.forEach(btn => {
    btn.addEventListener('click', () => {
        stack.push(Number(btn.dataset.chip));
        render();
    });
});

// ⑥ クリア
document.getElementById('clearBtn').addEventListener('click', () => {
    stack = [];
    render();
});

// ⑥ オールイン：大きいチップから順に、所持チップぴったりになるまで積む
document.getElementById('allInBtn').addEventListener('click', () => {
    stack = [];
    let rest = maxChips;
    for (const value of [500, 250, 100, 50, 25, 10, 5]) {
        while (rest >= value) {
            stack.push(value);
            rest -= value;
        }
    }
    // 5未満の端数（1〜4）が残ったら、その額のチップとして積む
    // （所持チップが2などの時でも、全額を賭けられるようにするため）
    if (rest > 0) {
        stack.push(rest);
    }
    render();
});

render(); // 最初の表示
