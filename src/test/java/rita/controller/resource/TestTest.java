package rita.controller.resource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class LearnTest {

    @Test
    void contextLoads() {
    }

    @Test
    void concatTest() {

        String a = "Пи";
        String b = "ська";

        assertEquals("Писька", a + b);

    }
}
