import java.util.ArrayList;
import java.util.List;

public class IngredientsDto {
    private List<String> ingredients = new ArrayList<>();

    public IngredientsDto(List<String> ingredients) {
        this.ingredients = ingredients;
    }

    public IngredientsDto(){

    }
    public List<String> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<String> ingredients) {
        this.ingredients = ingredients;
    }

    public void add(String s) {
        ingredients.add(s);
    }
}
