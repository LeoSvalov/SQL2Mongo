package tests;
import com.company.Solution;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SolutionTest {

    @Test
    void getSolution() {
        Solution test1 = new Solution("SELECT * FROM sales LIMIT 10");
        assertEquals("db.sales.find({}).limit(10);",test1.getSolution());

        Solution test2 = new Solution("SELECT name, surname FROM collection;");
        assertEquals("db.collection.find({}, {name: 1, surname: 1});", test2.getSolution());

        Solution test3 =  new Solution("SELECT   field    from    collection   limit 1  ;");
        assertEquals("db.collection.find({}, {field: 1}).limit(1);", test3.getSolution());

        Solution test4 = new Solution("SELECT * FROM customers WHERE age > 22 AND name = 'me'");
        assertEquals("db.customers.find({ $and: [ {\"age\": {$gt: 22}} , {\"name\": {$eq: 'me'}} ] });", test4.getSolution());

        Solution test5 = new Solution("select BuzzLightyear, Woody from Toy_Story where Imperator_Zurg=dead and Mr_Potato<>Mrs_Potato and Bulzay>Dinosaur_Rex skip 10 LIMIT 5;");
        assertEquals("db.toy_story.find({ $and: [ {\"imperator_zurg\": {$eq: dead}} , {\"mr_potato\": {$ne: mrs_potato}} , {\"bulzay\": {$gt: dinosaur_rex}} ] }, {buzzlightyear: 1, woody: 1}).skip(10).limit(5);"
        ,test5.getSolution());

    }
}