package nextstep.subway.favorite;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import nextstep.subway.AcceptanceTest;
import nextstep.subway.auth.acceptance.AuthAcceptanceTest;
import nextstep.subway.auth.dto.TokenResponse;
import nextstep.subway.favorites.dto.FavoritesResponse;
import nextstep.subway.line.acceptance.LineAcceptanceTest;
import nextstep.subway.line.dto.LineRequest;
import nextstep.subway.line.dto.LineResponse;
import nextstep.subway.member.MemberAcceptanceTest;
import nextstep.subway.member.dto.MemberResponse;
import nextstep.subway.station.StationAcceptanceTest;
import nextstep.subway.station.dto.StationResponse;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("즐겨찾기 관련 기능")
public class FavoritesAcceptanceTest extends AcceptanceTest {

    public static final String EMAIL = "hglee";
    public static final String PASSWORD = "1234";
    private StationResponse 양재역;
    private StationResponse 광교역;
    private StationResponse 고속터미널;
    private String tokenResponse;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        // given
        // 지하철역 등록 되어 있음
        양재역 = StationAcceptanceTest.지하철역_등록되어_있음("양재역").as(StationResponse.class);
        고속터미널 = StationAcceptanceTest.지하철역_등록되어_있음("고속터미널").as(StationResponse.class);
        광교역 = StationAcceptanceTest.지하철역_등록되어_있음("광교역").as(StationResponse.class);
        LineRequest 신분당선 = new LineRequest("신분당선", "red", 양재역.getId(), 광교역.getId(), 10);
        LineRequest 삼호선 = new LineRequest("삼호선", "red", 양재역.getId(), 고속터미널.getId(), 10);
        // 지하철 노선 등록 되어 있음
        // 지하철 노선에 지하철역 등록되어 있음
        LineAcceptanceTest.지하철_노선_등록되어_있음(신분당선);
        LineAcceptanceTest.지하철_노선_등록되어_있음(삼호선);
        // 회원 등록되어 있음
        MemberAcceptanceTest.회원_생성을_요청(EMAIL, PASSWORD, 30);
        // 로그인 되어있음
        tokenResponse = AuthAcceptanceTest.토큰_발급_요청(EMAIL, PASSWORD).as(TokenResponse.class).getAccessToken();
    }

    @DisplayName("즐겨찾기 생성 요청")
    @Test
    void addFavorite() {
        // when
        ExtractableResponse<Response> response = 즐겨찾기_생성_요청(양재역.getId(), 광교역.getId(), tokenResponse);

        // then
        Assertions.assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    }

    private ExtractableResponse<Response> 즐겨찾기_생성_요청(Long source, Long target, String token) {
        Map<String, String> params = new HashMap<>();
        params.put("source", source + "");
        params.put("target", target + "");
        return RestAssured.given().auth().oauth2(token).log().all().
                body(params).
                contentType(MediaType.APPLICATION_JSON_VALUE).
                when().
                post("/favorites").
                then().
                log().all().
                extract();
    }

    @DisplayName("즐겨찾기 목록 조회 요청")
    @Test
    void findAllFavorite() {
        // given
        // 즐겨찾기 생성됨
        ExtractableResponse<Response> responseFavorite1 = 즐겨찾기_생성_요청(양재역.getId(), 광교역.getId(), tokenResponse);
        ExtractableResponse<Response> responseFavorite2 = 즐겨찾기_생성_요청(양재역.getId(), 고속터미널.getId(), tokenResponse);

        // when
        ExtractableResponse<Response> response = 즐겨찾기_목록_조회_요청(tokenResponse);

        // then
        List<Long> expectedFavoritesIds = expectedFavoritesIds(responseFavorite1, responseFavorite2);
        List<Long> resultFavoritesIds = findFavoritesIds(response);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(resultFavoritesIds).containsAll(expectedFavoritesIds);
    }

    private List<Long> findFavoritesIds(ExtractableResponse<Response> response) {
        return response.jsonPath().getList(".", FavoritesResponse.class).stream()
                .map(FavoritesResponse::getId)
                .collect(Collectors.toList());
    }

    private List<Long> expectedFavoritesIds(ExtractableResponse<Response> responseFavorite1, ExtractableResponse<Response> responseFavorite2) {
        return Stream.of(responseFavorite1, responseFavorite2)
                .map(it -> Long.parseLong(it.header("Location").split("/")[2]))
                .collect(Collectors.toList());
    }

    private ExtractableResponse<Response> 즐겨찾기_목록_조회_요청(String token) {
        return RestAssured.given().auth().oauth2(token).log().all().
                when().
                accept(MediaType.APPLICATION_JSON_VALUE).
                get("/favorites").
                then().
                log().all().
                extract();
    }

    @DisplayName("즐겨찾기 삭제 요청")
    @Test
    void removeFavorite() {
        // when
        long favoriteId = 1L;
        ExtractableResponse<Response> response = 즐겨찾기_삭제_요청(favoriteId);

        // then
        Assertions.assertThat(response.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());

    }

    private ExtractableResponse<Response> 즐겨찾기_삭제_요청(long favoriteId) {
        return RestAssured.given().log().all().
                when().
                delete("/favorites/{id}", favoriteId + "").
                then().
                log().all().
                extract();
    }
}