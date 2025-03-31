package com.renzzle.backend.domain.puzzle.community.dao;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.renzzle.backend.domain.puzzle.community.api.request.GetCommunityPuzzleRequest;
import com.renzzle.backend.domain.puzzle.community.domain.CommunityPuzzle;
import com.renzzle.backend.domain.puzzle.community.domain.QCommunityPuzzle;
import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import com.renzzle.backend.global.common.constant.SortOption;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.renzzle.backend.domain.puzzle.community.domain.QCommunityPuzzle.communityPuzzle;
import static com.renzzle.backend.domain.puzzle.community.domain.QUserCommunityPuzzle.userCommunityPuzzle;
import static com.renzzle.backend.domain.user.domain.QUserEntity.userEntity;

@Repository
@RequiredArgsConstructor
public class CommunityPuzzleQueryRepositoryImpl implements CommunityPuzzleQueryRepository {

    @Autowired
    private JPAQueryFactory queryFactory;
    QCommunityPuzzle p2 = new QCommunityPuzzle("p2");

    @Override
    public List<CommunityPuzzle> searchCommunityPuzzles(GetCommunityPuzzleRequest request, Long userId) {
        return queryFactory
                .selectFrom(communityPuzzle)
                .join(communityPuzzle.user, userEntity)
                .where(
                        idCursorCondition(request.id(), request.sort()),
                        stoneEq(request.stone()),
                        authEq(request.auth()),
                        depthBetween(request.depthMin(), request.depthMax()),
                        solvedCondition(request.solved(), userId),
                        queryCondition(request.query())
                )
                .orderBy(orderSpecifier(request.sort()))
                .limit(size(request.size()))
                .fetch();
    }

    private BooleanExpression idCursorCondition(Long id, String sort) {
        SortOption sortOption;
        if (sort != null) {
            sortOption = SortOption.valueOf(sort);
        } else {
            sortOption = SortOption.LATEST;
        }

        if (id == null) return null;

        return switch (sortOption) {
            case LATEST -> communityPuzzle.createdAt.lt(
                    JPAExpressions.select(p2.createdAt)
                            .from(p2)
                            .where(p2.id.eq(id))
            ).or(
                    communityPuzzle.createdAt.eq(
                            JPAExpressions.select(p2.createdAt)
                                    .from(p2)
                                    .where(p2.id.eq(id))
                    ).and(communityPuzzle.id.gt(id))
            );
            case LIKE -> communityPuzzle.likeCount.lt(
                    JPAExpressions.select(p2.likeCount)
                            .from(p2)
                            .where(p2.id.eq(id))
            ).or(
                    communityPuzzle.likeCount.eq(
                            JPAExpressions.select(p2.likeCount)
                                    .from(p2)
                                    .where(p2.id.eq(id))
                    ).and(communityPuzzle.id.gt(id))
            );
        };
    }

    private BooleanExpression stoneEq(String stone) {
        return stone != null ? communityPuzzle.winColor.eq(WinColor.getWinColor(stone)) : null;
    }

    private BooleanExpression authEq(Boolean auth) {
        return auth != null ? communityPuzzle.isVerified.eq(auth) : null;
    }

    private BooleanExpression depthBetween(Integer min, Integer max) {
        int minVal = min != null ? min : 1;
        int maxVal = max != null ? max : 225;
        return communityPuzzle.depth.between(minVal, maxVal);
    }

    private BooleanExpression solvedCondition(Boolean solved, Long userId) {
        if (solved == null || userId == null) return null;

        JPQLQuery<Integer> subQuery = JPAExpressions
                .selectOne()
                .from(userCommunityPuzzle)
                .where(
                        userCommunityPuzzle.user.id.eq(userId),
                        userCommunityPuzzle.puzzle.id.eq(communityPuzzle.id)
                );

        return solved
                ? subQuery.exists()
                : subQuery.notExists();
    }

    private BooleanExpression queryCondition(String query) {
        if (query == null || query.isBlank()) return null;

        BooleanExpression byId = communityPuzzle.id.stringValue().eq(query);
        BooleanExpression byAuthor = communityPuzzle.user.nickname.containsIgnoreCase(query);
        return byId.or(byAuthor);
    }

    private OrderSpecifier<?>[] orderSpecifier(String sort) {
        SortOption sortOption;
        if (sort != null) {
            sortOption = SortOption.valueOf(sort);
        } else {
            sortOption = SortOption.LATEST;
        }

        return switch (sortOption) {
            case LATEST -> new OrderSpecifier[]{
                    communityPuzzle.createdAt.desc(),
                    communityPuzzle.id.asc()
            };
            case LIKE -> new OrderSpecifier[]{
                    communityPuzzle.likeCount.desc(),
                    communityPuzzle.id.asc()
            };
        };
    }

    private int size(Integer size) {
        return size != null ? size : 10;
    }

}
