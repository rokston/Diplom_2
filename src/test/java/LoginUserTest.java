import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

import com.github.javafaker.Faker;
import io.restassured.RestAssured;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.logging.Logger;

import io.qameta.allure.junit4.DisplayName; // импорт DisplayName
import io.qameta.allure.Step; // импорт Step

public class LoginUserTest {
    private static Logger log = Logger.getLogger(LoginUserTest.class.getName());
    Faker faker = new Faker();
    String email = faker.name().username() + "@testdomain.com";
    String password = faker.random().toString();
    String name = faker.name().firstName();

    //данные основного пользователя
    User newUser = new User(email, password, name);
    Credentials credentials = new Credentials(newUser.getEmail(), newUser.getPassword()); //логин и пароль основного пользователя

    @Test
    @DisplayName("Авторизация пользователя, успешная")
    public void loginUserOk() {
        createUser(newUser);
        Response response = loginUser(credentials);
        response.then()
                .statusCode(200);

    }

    @Test
    @DisplayName("Авторизация пользователя с неверным логином и существующим паролем, неуспешная")
    public void loginUserWrongEmailFail() {
        createUser(newUser);
        Credentials wrongLoginCredentials = new Credentials(newUser.getEmail() + "oops", newUser.getPassword());
        Response response = loginUser(wrongLoginCredentials);

        response.then()
                .statusCode(401);
        response.then()
                .assertThat().body("message", CoreMatchers.equalTo("email or password are incorrect"));
    }

    @Test
    @DisplayName("Авторизация пользователя с существующим логином и неверным паролем, неуспешная")
    public void loginUserWrongPasswordFail() {
        createUser(newUser);
        Credentials wrongLoginCredentials = new Credentials(newUser.getEmail(), newUser.getPassword() + "oops");
        Response response = loginUser(wrongLoginCredentials);
        response.then()
                .statusCode(401);
        response.then()
                .assertThat().body("message", CoreMatchers.equalTo("email or password are incorrect"));

    }

    @Step("Создание пользователя")
    public Response createUser(User user) {
        Response response =
                given()
                        .header("Content-type", "application/json")
                        .and()
                        .body(user)
                        .when()
                        .post(ApiEndpoint.CREATE_USER);

        return response;
    }

    @Step("логин пользователя")
    public Response loginUser(Credentials credentials) {
        Response response =
                given()
                        .header("Content-type", "application/json")
                        .and()
                        .body(credentials)
                        .when()
                        .post(ApiEndpoint.LOGIN_USER);
        return response;
    }

    @Step("авторизация пользователя и получение токена")
    public String loginUser(User user) { //авторизация пользователя и получение токена
        Credentials credentials = new Credentials(user.getEmail(), user.getPassword());
        Response response =
                given()
                        .header("Content-type", "application/json")
                        .and()
                        .body(credentials)
                        .when()
                        .post(ApiEndpoint.LOGIN_USER);
        int code = response.then().
                extract().statusCode();
        String userToken;
        if (code == 200) {
            userToken = response
                    .then().extract().body().path("accessToken");
        } else {
            userToken = null;
        }
        return userToken;
    }

    @Before
    public void setUp() {
        RestAssured.baseURI = ApiEndpoint.BASE_ADDRESS;
    }

    @After
    public void cleanUp() { //удаление пользователя
        User newUser = new User(email, password, name);
        String userToken = loginUser(newUser);
        if (userToken != null) {
            Response response = given()
                    .header("Content-type", "application/json")
                    .header("Authorization", userToken)
                    .when()
                    .delete(ApiEndpoint.DELETE_USER);

            response.then().log().all()
                    .assertThat()
                    .statusCode(202);
        } else {
            log.info("Cannot delete not existing user");
        }
    }
}
