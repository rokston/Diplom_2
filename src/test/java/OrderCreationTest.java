import io.qameta.allure.junit4.DisplayName;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

import com.github.javafaker.Faker;
import io.restassured.RestAssured;
import org.hamcrest.CoreMatchers;
import org.junit.*;

import java.util.logging.Logger;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;

import io.qameta.allure.Step;


public class OrderCreationTest {
    private static Logger log = Logger.getLogger(OrderCreationTest.class.getName());
    Faker faker = new Faker();
    String email = faker.name().username() + "@testdomain.com";
    String password = faker.random().toString();
    String name = faker.name().firstName();

    //данные основного пользователя
    User newUser = new User(email, password, name);
    Credentials credentials = new Credentials(newUser.getEmail(), newUser.getPassword()); //логин и пароль основного пользователя

    @Test
    @DisplayName("Создание заказа с ингредиентами и авторизацией")
    public void createOrderWithAuthTest() { //создаем заказ с ингредиентами и авторизацией
        createUser(newUser); //создаем нового пользователя
        String userToken = loginUser(newUser);
        IngredientsDto ingredientsDto = OrderData.prepareTestDataForOrder();
        //создаем заказ
        if (userToken != null) {
            Response response = given()
                    .header("Content-type", "application/json")
                    .header("Authorization", userToken)
                    .body(ingredientsDto)
                    .when()
                    .post(ApiEndpoint.CREATE_ORDER);

            response.then().log().all()
                    .assertThat()
                    .statusCode(200); //код ответа успешного запроса

            response.then()
                    .assertThat().body("success", equalTo(true)); //заказ успешен
            response.then().assertThat().body("order", notNullValue()); // поле order в ответе не пустое
            response.then().assertThat().body("order.owner", notNullValue()); // поле в ответе не пустое
            response.then().assertThat().body("order.number", notNullValue());// поле в ответе не пустое

        }
    }

    @Test
    @DisplayName("Создание заказа с ингредиентами и без авторизации")
    public void createOrderWithoutAuthTest() { //создаем заказ без авторизации
        createUser(newUser); //создаем нового пользователя
        IngredientsDto ingredientsDto = OrderData.prepareTestDataForOrder();
        //создаем заказ
        Response response = given()
                .header("Content-type", "application/json")
                .body(ingredientsDto)
                .when()
                .post(ApiEndpoint.CREATE_ORDER);

        response.then().log().all()
                .assertThat()
                .statusCode(200); //запрос обработан успешно

        response.then()
                .assertThat().body("success", equalTo(true));//запрос обработан успешно
        response.then().assertThat().body("order", notNullValue());
        response.then().assertThat().body("order.owner", equalTo(null)); // у заказа нет пользователя
        response.then().assertThat().body("order.number", notNullValue()); //у заказа есть номер
    }

    @Test
    @DisplayName("Создание заказа без ингредиентов и без авторизации")
    public void createOrderWithoutIngrNoAuthTest() { //создание заказа без ингредиентов и без авторизации
        createUser(newUser); //создаем нового пользователя
        String json = "{\"ingredients\": []}"; //в запросе не указаны id ингредиентов
        //создаем заказ
        Response response = given()
                .header("Content-type", "application/json")
                .body(json)
                .when()
                .post(ApiEndpoint.CREATE_ORDER);

        response.then().log().all() //ошибка в ответ на запрос
                .assertThat()
                .statusCode(400);

        response.then()
                .assertThat().body("success", equalTo(false));
        response.then().assertThat()
                .body("message", CoreMatchers.equalTo("Ingredient ids must be provided"));

    }

    @Test
    @DisplayName("Создание заказа без ингредиентов и с авторизацией")
    public void createOrderWithoutIngrWithAuthTest() { //создание заказа без ингредиентов и с авторизацией
        createUser(newUser); //создаем нового пользователя
        String json = "{\"ingredients\": []}"; //список ингредиентов без id

        //создаем заказ
        String userToken = loginUser(newUser);
        if (userToken != null) {
            Response response = given()
                    .header("Content-type", "application/json")
                    .header("Authorization", userToken)
                    .body(json)
                    .when()
                    .post(ApiEndpoint.CREATE_ORDER);

            response.then().log().all()//ошибка в ответ на запрос
                    .assertThat()
                    .statusCode(400);

            response.then()
                    .assertThat().body("success", equalTo(false));
            response.then().assertThat()
                    .body("message", CoreMatchers.equalTo("Ingredient ids must be provided"));


        }
    }

    @Test
    @DisplayName("Создание заказа с неверными id ингредиентов и авторизацией")
    public void createOrderWrongHashWithAuthTest() { //заказ с неверными id ингредиентов и авторизацией
        createUser(newUser); //создаем нового пользователя
        IngredientsDto ingredientsDto = OrderData.prepareWrongTestDataForOrder();
        //создаем заказ
        String userToken = loginUser(newUser);
        if (userToken != null) {
            Response response = given()
                    .header("Content-type", "application/json")
                    .header("Authorization", userToken)
                    .body(ingredientsDto)
                    .when()
                    .post(ApiEndpoint.CREATE_ORDER);

            response.then().log().all() //ошибка в ответ на запрос
                    .assertThat()
                    .statusCode(500);

        }
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

    @Step("логин пользователя и получение токена")
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



