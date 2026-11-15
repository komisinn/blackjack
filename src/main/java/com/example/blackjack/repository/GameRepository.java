package com.example.blackjack.repository;

import com.example.blackjack.entity.Game;
import com.example.blackjack.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

// gamesテーブルを操作するリポジトリ
public interface GameRepository extends JpaRepository<Game, Long> {

    // あるユーザーの最新3ゲーム（G6 右上の結果ログ用）
    List<Game> findTop3ByUserOrderByPlayedAtDesc(User user);

    // あるユーザーの最新30ゲーム（G8 チップ推移グラフ用）
    List<Game> findTop30ByUserOrderByPlayedAtDesc(User user);

    // あるユーザーの総ゲーム数（プレイ数）
    long countByUser(User user);

    // あるユーザーの、指定した結果（"WIN"など）のゲーム数
    long countByUserAndResult(User user, String result);

    // あるユーザーのチップ収支の合計（増減＋ボーナス）
    // coalesce は「1件もなくて null になる時は 0 にする」という意味
    @Query("select coalesce(sum(g.net + g.bonus), 0) from Game g where g.user = :user")
    long sumNetByUser(@Param("user") User user);

    // 【管理者用 G13】全ユーザーのプレイ履歴を、ユーザー名と期間で絞り込んでページごとに取得
    // "join fetch" でユーザー情報も一緒に読み込む（画面でユーザー名を表示するため）
    // ページ分けする時は「全部で何件あるか」を数える countQuery も必要
    @Query(value = "select g from Game g join fetch g.user u "
                 + "where (:name = '' or lower(u.userName) like lower(concat('%', :name, '%'))) "
                 + "  and g.playedAt >= :from and g.playedAt < :to "
                 + "order by g.playedAt desc",
           countQuery = "select count(g) from Game g join g.user u "
                 + "where (:name = '' or lower(u.userName) like lower(concat('%', :name, '%'))) "
                 + "  and g.playedAt >= :from and g.playedAt < :to")
    Page<Game> searchHistory(@Param("name") String name,
                             @Param("from") LocalDateTime from,
                             @Param("to") LocalDateTime to,
                             Pageable pageable);
}
