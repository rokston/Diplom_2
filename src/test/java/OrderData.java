import io.qameta.allure.Step;
import io.restassured.response.Response;

import java.util.List;
import java.util.Random;

import static io.restassured.RestAssured.given;

public class OrderData {
    @Step("получение списка ингредиентов")
    public static List<Data> getListOfIngredients() { //получение списка ингредиентов
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
    public static IngredientsDto prepareTestDataForOrder(){
        Random rn;
        int randomNum1, randomNum2, randomNum3;
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
    public static IngredientsDto prepareWrongTestDataForOrder(){
        Random rn;
        int randomNum1, randomNum2, randomNum3;
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

    @Step("выбор ингредиента для заказа с пользователем")
    public static IngredientsDto prepareIngredient(){
        List<Data> ingrList = getListOfIngredients(); //список доступных ингредиентов
        int size = ingrList.size(); //индексы массива будут от 0 до size-1
        Random rn = new Random();
        int randomNum1 = rn.nextInt(size);
        IngredientsDto ingredientsDto = new IngredientsDto(); //заполняем массив ингредиентов для заказа
        ingredientsDto.add(ingrList.get(randomNum1).get_id());
        return ingredientsDto;
    }

}
