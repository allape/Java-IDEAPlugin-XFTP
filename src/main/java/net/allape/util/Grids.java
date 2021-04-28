package net.allape.util;

import java.awt.*;

/**
 * {@link GridLayout}的预设坐标系, 不要直接更改对象的内容, 需要使用到动态的更改的布局时需要重新去实例化一个{@link GridBagConstraints}
 var a = new Array(10);
 var all = [];
 for (var x = 0; x < a.length; x++) {
    for (var y = 0; y < a.length; y++) {
        var varName = `X${x}Y${y}`;
        all.push(`
     public static final GridBagConstraints ${varName} = defaultConfig();
     static {
         ${varName}.gridx = ${x};
         ${varName}.gridy = ${y};
     }
 `);
     }
 }
 all.join('');
 */
public final class Grids {

    // region fast delete

    public static final GridBagConstraints X0Y0 = defaultConfig();
    static {
        X0Y0.gridx = 0;
        X0Y0.gridy = 0;
    }

    public static final GridBagConstraints X0Y1 = defaultConfig();
    static {
        X0Y1.gridx = 0;
        X0Y1.gridy = 1;
    }

    public static final GridBagConstraints X0Y2 = defaultConfig();
    static {
        X0Y2.gridx = 0;
        X0Y2.gridy = 2;
    }

    public static final GridBagConstraints X0Y3 = defaultConfig();
    static {
        X0Y3.gridx = 0;
        X0Y3.gridy = 3;
    }

    public static final GridBagConstraints X0Y4 = defaultConfig();
    static {
        X0Y4.gridx = 0;
        X0Y4.gridy = 4;
    }

    public static final GridBagConstraints X0Y5 = defaultConfig();
    static {
        X0Y5.gridx = 0;
        X0Y5.gridy = 5;
    }

    public static final GridBagConstraints X0Y6 = defaultConfig();
    static {
        X0Y6.gridx = 0;
        X0Y6.gridy = 6;
    }

    public static final GridBagConstraints X0Y7 = defaultConfig();
    static {
        X0Y7.gridx = 0;
        X0Y7.gridy = 7;
    }

    public static final GridBagConstraints X0Y8 = defaultConfig();
    static {
        X0Y8.gridx = 0;
        X0Y8.gridy = 8;
    }

    public static final GridBagConstraints X0Y9 = defaultConfig();
    static {
        X0Y9.gridx = 0;
        X0Y9.gridy = 9;
    }

    public static final GridBagConstraints X1Y0 = defaultConfig();
    static {
        X1Y0.gridx = 1;
        X1Y0.gridy = 0;
    }

    public static final GridBagConstraints X1Y1 = defaultConfig();
    static {
        X1Y1.gridx = 1;
        X1Y1.gridy = 1;
    }

    public static final GridBagConstraints X1Y2 = defaultConfig();
    static {
        X1Y2.gridx = 1;
        X1Y2.gridy = 2;
    }

    public static final GridBagConstraints X1Y3 = defaultConfig();
    static {
        X1Y3.gridx = 1;
        X1Y3.gridy = 3;
    }

    public static final GridBagConstraints X1Y4 = defaultConfig();
    static {
        X1Y4.gridx = 1;
        X1Y4.gridy = 4;
    }

    public static final GridBagConstraints X1Y5 = defaultConfig();
    static {
        X1Y5.gridx = 1;
        X1Y5.gridy = 5;
    }

    public static final GridBagConstraints X1Y6 = defaultConfig();
    static {
        X1Y6.gridx = 1;
        X1Y6.gridy = 6;
    }

    public static final GridBagConstraints X1Y7 = defaultConfig();
    static {
        X1Y7.gridx = 1;
        X1Y7.gridy = 7;
    }

    public static final GridBagConstraints X1Y8 = defaultConfig();
    static {
        X1Y8.gridx = 1;
        X1Y8.gridy = 8;
    }

    public static final GridBagConstraints X1Y9 = defaultConfig();
    static {
        X1Y9.gridx = 1;
        X1Y9.gridy = 9;
    }

    public static final GridBagConstraints X2Y0 = defaultConfig();
    static {
        X2Y0.gridx = 2;
        X2Y0.gridy = 0;
    }

    public static final GridBagConstraints X2Y1 = defaultConfig();
    static {
        X2Y1.gridx = 2;
        X2Y1.gridy = 1;
    }

    public static final GridBagConstraints X2Y2 = defaultConfig();
    static {
        X2Y2.gridx = 2;
        X2Y2.gridy = 2;
    }

    public static final GridBagConstraints X2Y3 = defaultConfig();
    static {
        X2Y3.gridx = 2;
        X2Y3.gridy = 3;
    }

    public static final GridBagConstraints X2Y4 = defaultConfig();
    static {
        X2Y4.gridx = 2;
        X2Y4.gridy = 4;
    }

    public static final GridBagConstraints X2Y5 = defaultConfig();
    static {
        X2Y5.gridx = 2;
        X2Y5.gridy = 5;
    }

    public static final GridBagConstraints X2Y6 = defaultConfig();
    static {
        X2Y6.gridx = 2;
        X2Y6.gridy = 6;
    }

    public static final GridBagConstraints X2Y7 = defaultConfig();
    static {
        X2Y7.gridx = 2;
        X2Y7.gridy = 7;
    }

    public static final GridBagConstraints X2Y8 = defaultConfig();
    static {
        X2Y8.gridx = 2;
        X2Y8.gridy = 8;
    }

    public static final GridBagConstraints X2Y9 = defaultConfig();
    static {
        X2Y9.gridx = 2;
        X2Y9.gridy = 9;
    }

    public static final GridBagConstraints X3Y0 = defaultConfig();
    static {
        X3Y0.gridx = 3;
        X3Y0.gridy = 0;
    }

    public static final GridBagConstraints X3Y1 = defaultConfig();
    static {
        X3Y1.gridx = 3;
        X3Y1.gridy = 1;
    }

    public static final GridBagConstraints X3Y2 = defaultConfig();
    static {
        X3Y2.gridx = 3;
        X3Y2.gridy = 2;
    }

    public static final GridBagConstraints X3Y3 = defaultConfig();
    static {
        X3Y3.gridx = 3;
        X3Y3.gridy = 3;
    }

    public static final GridBagConstraints X3Y4 = defaultConfig();
    static {
        X3Y4.gridx = 3;
        X3Y4.gridy = 4;
    }

    public static final GridBagConstraints X3Y5 = defaultConfig();
    static {
        X3Y5.gridx = 3;
        X3Y5.gridy = 5;
    }

    public static final GridBagConstraints X3Y6 = defaultConfig();
    static {
        X3Y6.gridx = 3;
        X3Y6.gridy = 6;
    }

    public static final GridBagConstraints X3Y7 = defaultConfig();
    static {
        X3Y7.gridx = 3;
        X3Y7.gridy = 7;
    }

    public static final GridBagConstraints X3Y8 = defaultConfig();
    static {
        X3Y8.gridx = 3;
        X3Y8.gridy = 8;
    }

    public static final GridBagConstraints X3Y9 = defaultConfig();
    static {
        X3Y9.gridx = 3;
        X3Y9.gridy = 9;
    }

    public static final GridBagConstraints X4Y0 = defaultConfig();
    static {
        X4Y0.gridx = 4;
        X4Y0.gridy = 0;
    }

    public static final GridBagConstraints X4Y1 = defaultConfig();
    static {
        X4Y1.gridx = 4;
        X4Y1.gridy = 1;
    }

    public static final GridBagConstraints X4Y2 = defaultConfig();
    static {
        X4Y2.gridx = 4;
        X4Y2.gridy = 2;
    }

    public static final GridBagConstraints X4Y3 = defaultConfig();
    static {
        X4Y3.gridx = 4;
        X4Y3.gridy = 3;
    }

    public static final GridBagConstraints X4Y4 = defaultConfig();
    static {
        X4Y4.gridx = 4;
        X4Y4.gridy = 4;
    }

    public static final GridBagConstraints X4Y5 = defaultConfig();
    static {
        X4Y5.gridx = 4;
        X4Y5.gridy = 5;
    }

    public static final GridBagConstraints X4Y6 = defaultConfig();
    static {
        X4Y6.gridx = 4;
        X4Y6.gridy = 6;
    }

    public static final GridBagConstraints X4Y7 = defaultConfig();
    static {
        X4Y7.gridx = 4;
        X4Y7.gridy = 7;
    }

    public static final GridBagConstraints X4Y8 = defaultConfig();
    static {
        X4Y8.gridx = 4;
        X4Y8.gridy = 8;
    }

    public static final GridBagConstraints X4Y9 = defaultConfig();
    static {
        X4Y9.gridx = 4;
        X4Y9.gridy = 9;
    }

    public static final GridBagConstraints X5Y0 = defaultConfig();
    static {
        X5Y0.gridx = 5;
        X5Y0.gridy = 0;
    }

    public static final GridBagConstraints X5Y1 = defaultConfig();
    static {
        X5Y1.gridx = 5;
        X5Y1.gridy = 1;
    }

    public static final GridBagConstraints X5Y2 = defaultConfig();
    static {
        X5Y2.gridx = 5;
        X5Y2.gridy = 2;
    }

    public static final GridBagConstraints X5Y3 = defaultConfig();
    static {
        X5Y3.gridx = 5;
        X5Y3.gridy = 3;
    }

    public static final GridBagConstraints X5Y4 = defaultConfig();
    static {
        X5Y4.gridx = 5;
        X5Y4.gridy = 4;
    }

    public static final GridBagConstraints X5Y5 = defaultConfig();
    static {
        X5Y5.gridx = 5;
        X5Y5.gridy = 5;
    }

    public static final GridBagConstraints X5Y6 = defaultConfig();
    static {
        X5Y6.gridx = 5;
        X5Y6.gridy = 6;
    }

    public static final GridBagConstraints X5Y7 = defaultConfig();
    static {
        X5Y7.gridx = 5;
        X5Y7.gridy = 7;
    }

    public static final GridBagConstraints X5Y8 = defaultConfig();
    static {
        X5Y8.gridx = 5;
        X5Y8.gridy = 8;
    }

    public static final GridBagConstraints X5Y9 = defaultConfig();
    static {
        X5Y9.gridx = 5;
        X5Y9.gridy = 9;
    }

    public static final GridBagConstraints X6Y0 = defaultConfig();
    static {
        X6Y0.gridx = 6;
        X6Y0.gridy = 0;
    }

    public static final GridBagConstraints X6Y1 = defaultConfig();
    static {
        X6Y1.gridx = 6;
        X6Y1.gridy = 1;
    }

    public static final GridBagConstraints X6Y2 = defaultConfig();
    static {
        X6Y2.gridx = 6;
        X6Y2.gridy = 2;
    }

    public static final GridBagConstraints X6Y3 = defaultConfig();
    static {
        X6Y3.gridx = 6;
        X6Y3.gridy = 3;
    }

    public static final GridBagConstraints X6Y4 = defaultConfig();
    static {
        X6Y4.gridx = 6;
        X6Y4.gridy = 4;
    }

    public static final GridBagConstraints X6Y5 = defaultConfig();
    static {
        X6Y5.gridx = 6;
        X6Y5.gridy = 5;
    }

    public static final GridBagConstraints X6Y6 = defaultConfig();
    static {
        X6Y6.gridx = 6;
        X6Y6.gridy = 6;
    }

    public static final GridBagConstraints X6Y7 = defaultConfig();
    static {
        X6Y7.gridx = 6;
        X6Y7.gridy = 7;
    }

    public static final GridBagConstraints X6Y8 = defaultConfig();
    static {
        X6Y8.gridx = 6;
        X6Y8.gridy = 8;
    }

    public static final GridBagConstraints X6Y9 = defaultConfig();
    static {
        X6Y9.gridx = 6;
        X6Y9.gridy = 9;
    }

    public static final GridBagConstraints X7Y0 = defaultConfig();
    static {
        X7Y0.gridx = 7;
        X7Y0.gridy = 0;
    }

    public static final GridBagConstraints X7Y1 = defaultConfig();
    static {
        X7Y1.gridx = 7;
        X7Y1.gridy = 1;
    }

    public static final GridBagConstraints X7Y2 = defaultConfig();
    static {
        X7Y2.gridx = 7;
        X7Y2.gridy = 2;
    }

    public static final GridBagConstraints X7Y3 = defaultConfig();
    static {
        X7Y3.gridx = 7;
        X7Y3.gridy = 3;
    }

    public static final GridBagConstraints X7Y4 = defaultConfig();
    static {
        X7Y4.gridx = 7;
        X7Y4.gridy = 4;
    }

    public static final GridBagConstraints X7Y5 = defaultConfig();
    static {
        X7Y5.gridx = 7;
        X7Y5.gridy = 5;
    }

    public static final GridBagConstraints X7Y6 = defaultConfig();
    static {
        X7Y6.gridx = 7;
        X7Y6.gridy = 6;
    }

    public static final GridBagConstraints X7Y7 = defaultConfig();
    static {
        X7Y7.gridx = 7;
        X7Y7.gridy = 7;
    }

    public static final GridBagConstraints X7Y8 = defaultConfig();
    static {
        X7Y8.gridx = 7;
        X7Y8.gridy = 8;
    }

    public static final GridBagConstraints X7Y9 = defaultConfig();
    static {
        X7Y9.gridx = 7;
        X7Y9.gridy = 9;
    }

    public static final GridBagConstraints X8Y0 = defaultConfig();
    static {
        X8Y0.gridx = 8;
        X8Y0.gridy = 0;
    }

    public static final GridBagConstraints X8Y1 = defaultConfig();
    static {
        X8Y1.gridx = 8;
        X8Y1.gridy = 1;
    }

    public static final GridBagConstraints X8Y2 = defaultConfig();
    static {
        X8Y2.gridx = 8;
        X8Y2.gridy = 2;
    }

    public static final GridBagConstraints X8Y3 = defaultConfig();
    static {
        X8Y3.gridx = 8;
        X8Y3.gridy = 3;
    }

    public static final GridBagConstraints X8Y4 = defaultConfig();
    static {
        X8Y4.gridx = 8;
        X8Y4.gridy = 4;
    }

    public static final GridBagConstraints X8Y5 = defaultConfig();
    static {
        X8Y5.gridx = 8;
        X8Y5.gridy = 5;
    }

    public static final GridBagConstraints X8Y6 = defaultConfig();
    static {
        X8Y6.gridx = 8;
        X8Y6.gridy = 6;
    }

    public static final GridBagConstraints X8Y7 = defaultConfig();
    static {
        X8Y7.gridx = 8;
        X8Y7.gridy = 7;
    }

    public static final GridBagConstraints X8Y8 = defaultConfig();
    static {
        X8Y8.gridx = 8;
        X8Y8.gridy = 8;
    }

    public static final GridBagConstraints X8Y9 = defaultConfig();
    static {
        X8Y9.gridx = 8;
        X8Y9.gridy = 9;
    }

    public static final GridBagConstraints X9Y0 = defaultConfig();
    static {
        X9Y0.gridx = 9;
        X9Y0.gridy = 0;
    }

    public static final GridBagConstraints X9Y1 = defaultConfig();
    static {
        X9Y1.gridx = 9;
        X9Y1.gridy = 1;
    }

    public static final GridBagConstraints X9Y2 = defaultConfig();
    static {
        X9Y2.gridx = 9;
        X9Y2.gridy = 2;
    }

    public static final GridBagConstraints X9Y3 = defaultConfig();
    static {
        X9Y3.gridx = 9;
        X9Y3.gridy = 3;
    }

    public static final GridBagConstraints X9Y4 = defaultConfig();
    static {
        X9Y4.gridx = 9;
        X9Y4.gridy = 4;
    }

    public static final GridBagConstraints X9Y5 = defaultConfig();
    static {
        X9Y5.gridx = 9;
        X9Y5.gridy = 5;
    }

    public static final GridBagConstraints X9Y6 = defaultConfig();
    static {
        X9Y6.gridx = 9;
        X9Y6.gridy = 6;
    }

    public static final GridBagConstraints X9Y7 = defaultConfig();
    static {
        X9Y7.gridx = 9;
        X9Y7.gridy = 7;
    }

    public static final GridBagConstraints X9Y8 = defaultConfig();
    static {
        X9Y8.gridx = 9;
        X9Y8.gridy = 8;
    }

    public static final GridBagConstraints X9Y9 = defaultConfig();
    static {
        X9Y9.gridx = 9;
        X9Y9.gridy = 9;
    }

    // endregion
    
    public static GridBagConstraints defaultConfig() {
        final GridBagConstraints gridBagCons = new GridBagConstraints();
        gridBagCons.fill = GridBagConstraints.BOTH;
        gridBagCons.weightx = 1;
        gridBagCons.weighty = 1;
        
        return gridBagCons;
    }

    public static GridBagConstraints xy(int x, int y) {
        GridBagConstraints gridBagCons = defaultConfig();
        gridBagCons.gridx = x;
        gridBagCons.gridy = y;
        return gridBagCons;
    }

}
