package io.itforge.nutrient.dagger;

import javax.inject.Qualifier;

public class Qualifiers {

    @Qualifier
    public @interface ForApplication {
    }

    @Qualifier
    public @interface ForActivity {
    }
}
