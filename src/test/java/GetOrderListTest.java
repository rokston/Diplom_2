import io.restassured.response.Response;
import static io.restassured.RestAssured.given;
import com.github.javafaker.Faker;
import io.restassured.RestAssured;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.*;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;

import io.qameta.allure.Step; // импорт Step

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GetOrderListTest {
    @Before
    public void setUp() {
        RestAssured.baseURI = ApiEndpoint.BASE_ADDRESS;
    }

    Faker faker = new Faker();
    String email = faker.name().username() + "@testdomain.com";
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

@Test
public void getAllOrdersTest(){
    Response response = given()
            .header("Content-type", "application/json")
           // .header("Authorization", userToken)
           // .body(json)
            // .body(ingredientsDto)
            .when()
            .get(ApiEndpoint.GET_ORDER_LIST);

    response.then().log().all()
            .assertThat()
            .statusCode(200);
    response.then()
            .assertThat().body("success", equalTo(true));
    response.then().assertThat().body("orders", notNullValue());
    response.then().assertThat().body("total", notNullValue());
    response.then().assertThat().body("totalToday", notNullValue());
}

   @Step("создание заказа с пользователем")
    public void createOrder(User user){
               String userToken = loginUser(user);
       if (userToken != null) {
           List<Data> ingrList = getListOfIngredients();
           int size = ingrList.size(); //индексы массива будут от 0 до size-1
           System.out.println(size);
           //System.out.println("this is size");

           //сгенерируем 3 случайных числа, это будут индексы ингредиентов

           Random rn = new Random();
           int randomNum1 = rn.nextInt(size);
           // System.out.println(randomNum1);

           String json = "{\"ingredients\": [" + "\""
                   + ingrList.get(randomNum1).get_id() + "\"" + "]}";

           Response response = given()
                   .header("Content-type", "application/json")
                   .header("Authorization", userToken)
                   .body(json)
                   .when()
                   .post(ApiEndpoint.CREATE_ORDER);

       }
}

    @Test
    public void getOrdersByUserTest(){
        createUser(newUser); //создаем нового пользователя
        String userToken = loginUser(newUser);
        Random rn = new Random();
        int randomNum = rn.nextInt(10)+1;
        for (int i = 0; i<randomNum; i++){
        createOrder(newUser);}

      Response      response = given()
                    .header("Content-type", "application/json")
                    .header("Authorization", userToken)
                    // .body(json)
                    // .body(ingredientsDto)
                    .when()
                    .get(ApiEndpoint.GET_ORDER_LIST_BY_USER);

            response.then().log().all()
                    .assertThat()
                    .statusCode(200);
            response.then()
                    .assertThat().body("success", equalTo(true));

        List<String> orders = response.then().extract().body().jsonPath().getList("orders._id");
        MatcherAssert.assertThat(orders.isEmpty(),is(false));
        System.out.println(orders.size());
        System.out.println("размер массива заказов");
        MatcherAssert.assertThat(orders.size(),is(randomNum));
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
            System.out.println("Cannot delete not existing user");
        }
    }

}
