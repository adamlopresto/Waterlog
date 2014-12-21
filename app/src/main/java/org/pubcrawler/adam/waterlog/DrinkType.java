package org.pubcrawler.adam.waterlog;

/**
* Created by adam on 12/21/2014.
*/
public enum DrinkType {
    Home(12), Work(16), Coffee(8);

    private final int oz;

    DrinkType(int oz) {
        this.oz = oz;
    }

    public int oz() {
        return oz;
    }
}
