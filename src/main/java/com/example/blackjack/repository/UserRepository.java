package com.example.blackjack.repository;

import com.example.blackjack.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// usersテーブルを操作するリポジトリ
// JpaRepositoryを継承するだけで save / findById / findAll / delete などが自動で使える
// メソッド名のルール（findBy〜, existsBy〜）に従うと、SQLをSpringが自動生成してくれる
public interface UserRepository extends JpaRepository<User, Long> {

    // メールアドレスでユーザーを1件探す（ログイン時に使う）
    // 見つからない場合もあるので Optional で包んで返す
    Optional<User> findByEmail(String email);

    // そのメールアドレスがすでに登録済みかどうか（新規登録の重複チェック）
    boolean existsByEmail(String email);

    // 指定ロールのユーザーが1人でもいるか（初期管理者を作るかの判定）
    boolean existsByRole(String role);

    // 指定ロールのユーザー数（G11 概況：登録ユーザー）
    long countByRole(String role);

    // 指定ロールで、有効／停止中のユーザー数（G11 概況：停止中）
    long countByRoleAndEnabled(String role, Boolean enabled);

    // チップの多い順に並べる（G8 チップランキング）
    List<User> findByRoleOrderByChipsDescUserIdAsc(String role);

    // 全ユーザーをID順に取得（G12 ユーザー管理）
    List<User> findAllByOrderByUserIdAsc();

    // ユーザー名またはメールアドレスの一部で検索（G12 検索）
    // lower() で大文字・小文字を区別しないようにしている
    @Query("select u from User u "
         + "where lower(u.userName) like lower(concat('%', :q, '%')) "
         + "   or lower(u.email) like lower(concat('%', :q, '%')) "
         + "order by u.userId")
    List<User> search(@Param("q") String q);
}
