import io.restassured.response.Response;
import static io.restassured.RestAssured.given;
import com.github.javafaker.Faker;
import io.restassured.RestAssured;
import org.hamcrest.CoreMatchers;
import org.junit.*;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;

import io.qameta.allure.Step; // импорт Step

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class OrderCreationTest {

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

    Random rn;
    int randomNum1, randomNum2, randomNum3;

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
    public void createOrderWithAuthTest() {
        createUser(newUser); //созадем нового пользователя
        Response response = loginUser(credentials); //логинимся
        List<Data> ingrList = getListOfIngredients();
        int size = ingrList.size(); //индексы массива будут от 0 до size-1
        System.out.println(size);
        System.out.println("this is size");

        //сгенерируем 3 случайных числа, это будут индексы ингредиентов

        rn = new Random();
        randomNum1 = rn.nextInt(size);
        System.out.println(randomNum1);

        rn = new Random();
        randomNum2 = rn.nextInt(size);
        System.out.println(randomNum2);

        rn = new Random();
        randomNum3 = rn.nextInt(size);
        System.out.println(randomNum3);

        //IngredientsDto ingredientsDto = new IngredientsDto();
      /*  IngredientsDto ingredientsDto = (IngredientsDto) List.of("\"" + ingrList.get(randomNum1).get_id()+ "\"");
        ingredientsDto.add( "\"" + ingrList.get(randomNum1).get_id()+ "\"");
        ingredientsDto.add( "\"" + ingrList.get(randomNum2).get_id()+ "\"");
        ingredientsDto.add( "\"" + ingrList.get(randomNum3).get_id()+ "\"");*/
       /* List<String> tempIngr = new ArrayList<>();
        tempIngr.add( "\"" + ingrList.get(randomNum1).get_id()+ "\"");
        tempIngr.add( "\"" + ingrList.get(randomNum2).get_id()+ "\"");
        tempIngr.add("\"" + ingrList.get(randomNum3).get_id()+ "\"");
        ingredientsDto.setIngredients(tempIngr);*/

/*        List<IngredientsDto> ingredientsDto = new ArrayList<IngredientsDto>();
        ingredientsDto.add(new IngredientsDto( "\"" + ingrList.get(randomNum1).get_id()+ "\""));
        ingredientsDto.add(new IngredientsDto( "\"" + ingrList.get(randomNum2).get_id()+ "\""));
        ingredientsDto.add(new IngredientsDto( "\"" + ingrList.get(randomNum3).get_id()+ "\""));
        System.out.println(ingredientsDto); */

        System.out.println("ingredients!!");

        String json = "{\"ingredients\": [" + "\""
                + ingrList.get(randomNum1).get_id() + "\"" + ", "
                + "\"" + ingrList.get(randomNum2).get_id() + "\"" + ", "
                +"\"" + ingrList.get(randomNum3).get_id() + "\"]}";
        System.out.println(json);
        //создаем заказ
        String userToken = loginUser(newUser);
        if (userToken != null) {
            response = given()
                    .header("Content-type", "application/json")
                    .header("Authorization", userToken)
                    .body(json)
                   // .body(ingredientsDto)
                    .when()
                    .post(ApiEndpoint.CREATE_ORDER);

            response.then().log().all()
                    .assertThat()
                    .statusCode(200);

            response.then()
                    .assertThat().body("success", equalTo(true));
            response.then().assertThat().body("order", notNullValue());
            response.then().assertThat().body("order.owner", notNullValue());
            response.then().assertThat().body("order.number", notNullValue());

        }
    }

    @Test
    public void createOrderWithoutAuthTest() {
        createUser(newUser); //созадем нового пользователя
        Response response = loginUser(credentials); //логинимся
        List<Data> ingrList = getListOfIngredients();
        int size = ingrList.size(); //индексы массива будут от 0 до size-1
        System.out.println(size);
        System.out.println("this is size");

        //сгенерируем 3 случайных числа, это будут индексы ингредиентов

        rn = new Random();
        randomNum1 = rn.nextInt(size);
        System.out.println(randomNum1);

        rn = new Random();
        randomNum2 = rn.nextInt(size);
        System.out.println(randomNum2);

        rn = new Random();
        randomNum3 = rn.nextInt(size);
        System.out.println(randomNum3);


        String json = "{\"ingredients\": [" + "\""
                + ingrList.get(randomNum1).get_id() + "\"" + ", "
                + "\"" + ingrList.get(randomNum2).get_id() + "\"" + ", "
                +"\"" + ingrList.get(randomNum3).get_id() + "\"]}";
        System.out.println(json);
        //создаем заказ

            response = given()
                    .header("Content-type", "application/json")
                    //.header("Authorization", userToken)
                    .body(json)
                    // .body(ingredientsDto)
                    .when()
                    .post(ApiEndpoint.CREATE_ORDER);

            response.then().log().all()
                    .assertThat()
                    .statusCode(200);

            response.then()
                    .assertThat().body("success", equalTo(true));
            response.then().assertThat().body("order", notNullValue());
            response.then().assertThat().body("order.owner", equalTo(null));
            response.then().assertThat().body("order.number", notNullValue());

    }


    @Test
    public void createOrderWithoutIngrNoAuthTest() {
        createUser(newUser); //созадем нового пользователя
        Response response = loginUser(credentials); //логинимся
        List<Data> ingrList = getListOfIngredients();
        int size = ingrList.size(); //индексы массива будут от 0 до size-1
        System.out.println(size);
        System.out.println("this is size");

        //сгенерируем 3 случайных числа, это будут индексы ингредиентов

  /*      String json = "{\"ingredients\": [" + "\""
                + ingrList.get(randomNum1).get_id() + "\"" + ", "
                + "\"" + ingrList.get(randomNum2).get_id() + "\"" + ", "
                +"\"" + ingrList.get(randomNum3).get_id() + "\"]}";*/
        String json = "{\"ingredients\": []}";
        System.out.println(json);
        //создаем заказ

        response = given()
                .header("Content-type", "application/json")
                //.header("Authorization", userToken)
                .body(json)
                // .body(ingredientsDto)
                .when()
                .post(ApiEndpoint.CREATE_ORDER);

        response.then().log().all()
                .assertThat()
                .statusCode(400);

        response.then()
                .assertThat().body("success", equalTo(false));
        response.then().assertThat().body("message", CoreMatchers.equalTo("Ingredient ids must be provided"));

    }

    @Test
    public void createOrderWithoutIngrWithAuthTest() {
        createUser(newUser); //созадем нового пользователя
        Response response = loginUser(credentials); //логинимся
        List<Data> ingrList = getListOfIngredients();
        int size = ingrList.size(); //индексы массива будут от 0 до size-1
        System.out.println(size);
        System.out.println("this is size");
/*        String json = "{\"ingredients\": [" + "\""
                + ingrList.get(randomNum1).get_id() + "\"" + ", "
                + "\"" + ingrList.get(randomNum2).get_id() + "\"" + ", "
                +"\"" + ingrList.get(randomNum3).get_id() + "\"]}";*/
        String json = "{\"ingredients\": []}";
        System.out.println(json);
        //создаем заказ
        String userToken = loginUser(newUser);
        if (userToken != null) {
            response = given()
                    .header("Content-type", "application/json")
                    .header("Authorization", userToken)
                    .body(json)
                    // .body(ingredientsDto)
                    .when()
                    .post(ApiEndpoint.CREATE_ORDER);

            response.then().log().all()
                    .assertThat()
                    .statusCode(400);

            response.then()
                    .assertThat().body("success", equalTo(false));
            response.then().assertThat().body("message", CoreMatchers.equalTo("Ingredient ids must be provided"));


        }
    }

    @Test
    public void createOrderWrongHashWithAuthTest() {
        createUser(newUser); //созадем нового пользователя
        Response response = loginUser(credentials); //логинимся
        List<Data> ingrList = getListOfIngredients();
        int size = ingrList.size(); //индексы массива будут от 0 до size-1
        System.out.println(size);
        System.out.println("this is size");
        String json = "{\"ingredients\": [" + "\""
                + ingrList.get(randomNum1).get_id() + "c" + "\"" + ", "
                + "\"" + ingrList.get(randomNum2).get_id() + "h" + "\"" + ", "
                +"\"" + ingrList.get(randomNum3).get_id() + "\"]}";
        //String json = "{\"ingredients\": []}";
        System.out.println(json);
        //создаем заказ
        String userToken = loginUser(newUser);
        if (userToken != null) {
            response = given()
                    .header("Content-type", "application/json")
                    .header("Authorization", userToken)
                    .body(json)
                    // .body(ingredientsDto)
                    .when()
                    .post(ApiEndpoint.CREATE_ORDER);

            response.then().log().all()
                    .assertThat()
                    .statusCode(500);


        }
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



