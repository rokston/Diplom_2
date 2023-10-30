import com.github.javafaker.Faker;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;

import io.qameta.allure.junit4.DisplayName; // импорт DisplayName
import io.qameta.allure.Description; // импорт Description
import io.qameta.allure.Step; // импорт Step

import java.io.File;
public class CreateUserTest {
    @Before
    public void setUp() {

        RestAssured.baseURI = ApiEndpoint.BASE_ADDRESS;
    }

    Faker faker = new Faker();
    String email = faker.name().username() +"@testdomain.com";
    String password = faker.random().toString();
    String name = faker.name().firstName();

    @Step("Создание пользователя")
    public Response createUser(User user)
    {
        Response response =
                given()
                        .header("Content-type", "application/json")
                        .and()
                        .body(user)
                        .when()
                        .post(ApiEndpoint.CREATE_USER);

        return response;
    }

    @Test
    @DisplayName("Создание пользователя, успешное")
    public void postCreateUser() {

        User newUser = new User(email, password, name);

        Response response = createUser(newUser);
        response.then()
                .statusCode(200);
        response.then()
                .assertThat().body("success", is(true));

    }


    @Test
    @DisplayName("Создание повторяющегося пользователя, неуспешное")
    public void postCreateSameUserFail() {

        User newUser = new User(email, password, name);
        Response response = createUser(newUser);
        Response responseFail = createUser(newUser);
        responseFail.then()
                .statusCode(403);
        responseFail.then()
                .assertThat().body("success", equalTo(false));
        responseFail.then()

                .assertThat().body("message", equalTo("User already exists"));

    }

    @Test
    @DisplayName("Создание пользователя с существующим логином  другим паролем, неуспешное")
    public void postCreateSameUserOtherPasswordFail() {

        User newUser = new User(email, password, name);

        User newUserOtherPassword = new User(newUser.getEmail(), newUser.getPassword() + "qq", newUser.getName());

        Response response = createUser(newUser);
        Response responseFail = createUser(newUserOtherPassword);

        responseFail.then()
                .statusCode(403);
        responseFail.then()
                .assertThat().body("success", equalTo(false));
        responseFail.then()

                .assertThat().body("message", equalTo("User already exists"));

    }

    @Test
    @DisplayName("Создание пользователя без пароля, неуспешное")
    public void postCreateUserWithoutPasswordFail() {

        User newUser = new User(email, "", name);
        Response responseFail = createUser(newUser);
        responseFail.then()
                .statusCode(403);

        responseFail.then()
                .assertThat().body("success", equalTo(false));
        responseFail.then()
                .assertThat().body("message", equalTo("Email, password and name are required fields"));

    }

    @Test
    @DisplayName("Создание пользователя без email, неуспешное")
    public void postCreateUserWithoutEmailFail() {

        User newUser = new User("", password, name);
        Response responseFail = createUser(newUser);
        responseFail.then()
                .statusCode(403);

        responseFail.then()
                .assertThat().body("success", equalTo(false));
        responseFail.then()
                .assertThat().body("message", equalTo("Email, password and name are required fields"));

    }

    @Test
    @DisplayName("Создание пользователя без имени, неуспешное")
    public void postCreateUserWithoutNameFail() {

        User newUser = new User(email, password, "");
        Response responseFail = createUser(newUser);
        responseFail.then()
                .statusCode(403);

        responseFail.then()
                .assertThat().body("success", equalTo(false));
        responseFail.then()
                .assertThat().body("message", equalTo("Email, password and name are required fields"));

    }

    public String loginUser(User user){ //авторизация пользователя, с целью получения токена

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
                    .then().extract().body().path("accessToken");}
        else {
            userToken = null;
        }
        return userToken;
    }


    @After
    public void cleanUp() { //удаление пользователя
        User newUser = new User(email, password, name);
        String userToken = loginUser(newUser);
        if (userToken != null)  {
            Response response = given()
                    .header("Content-type", "application/json")
                    .header("Authorization", userToken)
                    .when()
                    .delete(ApiEndpoint.DELETE_USER);

                    response.then().log().all()
                    .assertThat()
                    .statusCode(202);

            }
        else  {
            System.out.println("Cannot delete not existing user");
        }
    }
}
