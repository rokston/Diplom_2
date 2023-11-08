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
import java.util.List;
import java.util.Random;

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
        IngredientsDto ingredientsDto = prepareTestDataForOrder();
        //создаем заказ
         if (userToken != null) {
         Response   response = given()
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
        IngredientsDto ingredientsDto = prepareTestDataForOrder();
        //создаем заказ
        Response    response = given()
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

    @Test@DisplayName("Создание заказа без ингредиентов и с авторизацией")
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
        IngredientsDto ingredientsDto = prepareWrongTestDataForOrder();
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

    @Step("получение списка ингредиентов")
    public List<Data> getListOfIngredients() { //получение списка ингредиентов
        Response response =
                given()
                        .header("Content-type", "application/json")
                        .and()
                        .when()
                        .get(ApiEndpoint.GET_INGREDIENTS_LIST);
        response.then().log().all()
                .assertThat()
                .statusCode(200);
        Ingredients ingredients = response.body().as(Ingredients.class);

        return ingredients.getData();
    }


    @Step("Создание набора ингредиентов для формирования заказа")
    public IngredientsDto prepareTestDataForOrder(){
        Random rn;
        int randomNum1, randomNum2, randomNum3;
        createUser(newUser); //создаем нового пользователя
        Response response = loginUser(credentials); //логинимся
        List<Data> ingrList = getListOfIngredients();
        int size = ingrList.size(); //индексы массива будут от 0 до size-1
        //сгенерируем 3 случайных числа, это будут индексы ингредиентов
        rn = new Random();
        randomNum1 = rn.nextInt(size);
        rn = new Random();
        randomNum2 = rn.nextInt(size);
        rn = new Random();
        randomNum3 = rn.nextInt(size);
        IngredientsDto ingredientsDto = new IngredientsDto(); //заполняем массив ингредиентов для заказа
        ingredientsDto.add(ingrList.get(randomNum1).get_id());
        ingredientsDto.add(ingrList.get(randomNum2).get_id());
        ingredientsDto.add(ingrList.get(randomNum3).get_id());

        return ingredientsDto;
    }


    @Step("Создание набора ингредиентов для формирования заказа с неверным хешем")
    public IngredientsDto prepareWrongTestDataForOrder(){
        Random rn;
        int randomNum1, randomNum2, randomNum3;
        createUser(newUser); //создаем нового пользователя
        Response response = loginUser(credentials); //логинимся
        List<Data> ingrList = getListOfIngredients();
        int size = ingrList.size(); //индексы массива будут от 0 до size-1
        //сгенерируем 3 случайных числа, это будут индексы ингредиентов
        rn = new Random();
        randomNum1 = rn.nextInt(size);
        rn = new Random();
        randomNum2 = rn.nextInt(size);
        rn = new Random();
        randomNum3 = rn.nextInt(size);
        IngredientsDto ingredientsDto = new IngredientsDto(); //заполняем массив ингредиентов для заказа
        ingredientsDto.add(ingrList.get(randomNum1).get_id() + "pp");
        ingredientsDto.add(ingrList.get(randomNum2).get_id());
        ingredientsDto.add(ingrList.get(randomNum3).get_id());

        return ingredientsDto;
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



