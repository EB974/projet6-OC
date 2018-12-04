
package com.eric_b.go4lunch.modele.placeid;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Periodid {

    @SerializedName("close")
    @Expose
    private Closeid close;
    @SerializedName("open")
    @Expose
    private Openid open;

    public Closeid getClose() {
        return close;
    }

    public void setClose(Closeid close) {
        this.close = close;
    }

    public Openid getOpen() {
        return open;
    }

    public void setOpen(Openid open) {
        this.open = open;
    }

}
