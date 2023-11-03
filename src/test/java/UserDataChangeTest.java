import io.restassured.response.Response;
import static io.restassured.RestAssured.given;
import com.github.javafaker.Faker;
import io.restassured.RestAssured;
import org.hamcrest.CoreMatchers;
import org.junit.*;
import io.qameta.allure.Step; // импорт Step

public class UserDataChangeTest {

    @Before
    public void setUp() {
        RestAssured.baseURI = ApiEndpoint.BASE_ADDRESS;
    }
    Faker faker = new Faker();
    String email = faker.name().username() + "@testdomain.com";
    String chEmail = email + "ch";
    String password = faker.random().toString();
    String name = faker.name().firstName();

    //данные основного пользователя
    User newUser = new User(email, password, name);
    Credentials credentials = new Credentials(newUser.getEmail(), newUser.getPassword()); //логин и пароль основного пользователя


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


    @Step("удаление пользователя")
    public Response deleteUser(String userToken) {
        Response response = given()
                .header("Content-type", "application/json")
                .header("Authorization", userToken)
                .when()
                .delete(ApiEndpoint.DELETE_USER);
        return response;
    }

    @Step("Изменение данных пользователя")
    @Test
    public void changeUserDataTest() {
        createUser(newUser); //созадем нового пользователя
        Response response = loginUser(credentials); //логинимся
        UserData userData = new UserData(email + "ch", name + "ch"); //отредактированные данные пользователя
        int code = response.then().
                extract().statusCode();
        if (code == 200) { // логин прошел успешно, можно редактировать данные
            String userToken = response //токен для доступа на редактирование
                    .then().extract().body().path("accessToken");

            response = given() // устанавливаем новые данные пользователя
                    .header("Content-type", "application/json")
                    .header("Authorization", userToken)
                    .and()
                    .body(userData)
                    .when()
                    .patch(ApiEndpoint.CHANGE_USER_DATA);

            response.then().log().all() //проверяем, что запрос на редактирование принят успешно
                    .assertThat()
                    .statusCode(200);
//проверяем, что после редактирования данные пользователя изменились
            response.then().assertThat().body("user.email", CoreMatchers.equalTo(userData.getEmail()));
            response.then().assertThat().body("user.name", CoreMatchers.equalTo(userData.getName()));



            response = loginUser(credentials);//неудачная попытка залогиниться со старым имейлом
            response.then().log().all()
                    .assertThat()
                    .statusCode(401);

            credentials.setEmail(chEmail); //в данных пользователя для логина устанавливаем новый имейл

            response = loginUser(credentials); //удачная попытка логина с новыми даныыми

            response.then().log().all()
                    .assertThat()
                    .statusCode(200);

        }
    }

    @Step("Изменение данных пользователя без авторизации")
    @Test
    public void changeUserDataTestNoAuth() {
        createUser(newUser); //созадем нового пользователя
        Response response = loginUser(credentials); //логинимся
        UserData userData = new UserData(email + "ch", name + "ch"); //отредактированные данные пользователя
        int code = response.then().
                extract().statusCode();
        if (code == 200) { // логин прошел успешно, можно редактировать данные

            response = given() // устанавливаем новые данные пользователя
                    .header("Content-type", "application/json")
                    .and()
                    .body(userData)
                    .when()
                    .patch(ApiEndpoint.CHANGE_USER_DATA);

            response.then().log().all() //проверяем, что запрос на редактирование не принят
                    .assertThat()
                    .statusCode(401);



            response = loginUser(credentials);//удачная попытка залогиниться со старым имейлом
            response.then().log().all()
                    .assertThat()
                    .statusCode(200);
        }
    }


    @After
    public void cleanUp() { //удаление пользователя
        User newUser = new User(chEmail, password, name);//удаление пользователя с отредактированными данными
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
        else {
            newUser = new User(email, password, name);//удаление пользователя с неотредактированными данными
            userToken = loginUser(newUser);

            if (userToken != null) {
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
}