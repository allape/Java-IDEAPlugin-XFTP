package net.allape.utils;

import java.awt.*;

/**
 * {@link GridLayout}的预设坐标系, 不要直接更改对象的内容, 需要使用到动态的更改的布局时需要重新去实例化一个{@link GridBagConstraints}
 var a = new Array(10);
 var all = [];
 for (var x = 0; x < a.length; x++) {
    for (var y = 0; y < a.length; y++) {
        var varName = `X${x}Y${y}`;
        all.push(`
     public static final GridBagConstraints ${varName} = new GridBagConstraints();
     static {
         ${varName}.fill = GridBagConstraints.BOTH;
         ${varName}.gridx = ${x};
         ${varName}.gridy = ${y};
         ${varName}.weightx = 1;
         ${varName}.weighty = 1;
     }
 `);
     }
 }
 all.join('');
 */
public final class Grids {

    // region fast delete

    public static final GridBagConstraints X0Y0 = new GridBagConstraints();
    static {
        X0Y0.fill = GridBagConstraints.BOTH;
        X0Y0.gridx = 0;
        X0Y0.gridy = 0;
        X0Y0.weightx = 1;
        X0Y0.weighty = 1;
    }

    public static final GridBagConstraints X0Y1 = new GridBagConstraints();
    static {
        X0Y1.fill = GridBagConstraints.BOTH;
        X0Y1.gridx = 0;
        X0Y1.gridy = 1;
        X0Y1.weightx = 1;
        X0Y1.weighty = 1;
    }

    public static final GridBagConstraints X0Y2 = new GridBagConstraints();
    static {
        X0Y2.fill = GridBagConstraints.BOTH;
        X0Y2.gridx = 0;
        X0Y2.gridy = 2;
        X0Y2.weightx = 1;
        X0Y2.weighty = 1;
    }

    public static final GridBagConstraints X0Y3 = new GridBagConstraints();
    static {
        X0Y3.fill = GridBagConstraints.BOTH;
        X0Y3.gridx = 0;
        X0Y3.gridy = 3;
        X0Y3.weightx = 1;
        X0Y3.weighty = 1;
    }

    public static final GridBagConstraints X0Y4 = new GridBagConstraints();
    static {
        X0Y4.fill = GridBagConstraints.BOTH;
        X0Y4.gridx = 0;
        X0Y4.gridy = 4;
        X0Y4.weightx = 1;
        X0Y4.weighty = 1;
    }

    public static final GridBagConstraints X0Y5 = new GridBagConstraints();
    static {
        X0Y5.fill = GridBagConstraints.BOTH;
        X0Y5.gridx = 0;
        X0Y5.gridy = 5;
        X0Y5.weightx = 1;
        X0Y5.weighty = 1;
    }

    public static final GridBagConstraints X0Y6 = new GridBagConstraints();
    static {
        X0Y6.fill = GridBagConstraints.BOTH;
        X0Y6.gridx = 0;
        X0Y6.gridy = 6;
        X0Y6.weightx = 1;
        X0Y6.weighty = 1;
    }

    public static final GridBagConstraints X0Y7 = new GridBagConstraints();
    static {
        X0Y7.fill = GridBagConstraints.BOTH;
        X0Y7.gridx = 0;
        X0Y7.gridy = 7;
        X0Y7.weightx = 1;
        X0Y7.weighty = 1;
    }

    public static final GridBagConstraints X0Y8 = new GridBagConstraints();
    static {
        X0Y8.fill = GridBagConstraints.BOTH;
        X0Y8.gridx = 0;
        X0Y8.gridy = 8;
        X0Y8.weightx = 1;
        X0Y8.weighty = 1;
    }

    public static final GridBagConstraints X0Y9 = new GridBagConstraints();
    static {
        X0Y9.fill = GridBagConstraints.BOTH;
        X0Y9.gridx = 0;
        X0Y9.gridy = 9;
        X0Y9.weightx = 1;
        X0Y9.weighty = 1;
    }

    public static final GridBagConstraints X1Y0 = new GridBagConstraints();
    static {
        X1Y0.fill = GridBagConstraints.BOTH;
        X1Y0.gridx = 1;
        X1Y0.gridy = 0;
        X1Y0.weightx = 1;
        X1Y0.weighty = 1;
    }

    public static final GridBagConstraints X1Y1 = new GridBagConstraints();
    static {
        X1Y1.fill = GridBagConstraints.BOTH;
        X1Y1.gridx = 1;
        X1Y1.gridy = 1;
        X1Y1.weightx = 1;
        X1Y1.weighty = 1;
    }

    public static final GridBagConstraints X1Y2 = new GridBagConstraints();
    static {
        X1Y2.fill = GridBagConstraints.BOTH;
        X1Y2.gridx = 1;
        X1Y2.gridy = 2;
        X1Y2.weightx = 1;
        X1Y2.weighty = 1;
    }

    public static final GridBagConstraints X1Y3 = new GridBagConstraints();
    static {
        X1Y3.fill = GridBagConstraints.BOTH;
        X1Y3.gridx = 1;
        X1Y3.gridy = 3;
        X1Y3.weightx = 1;
        X1Y3.weighty = 1;
    }

    public static final GridBagConstraints X1Y4 = new GridBagConstraints();
    static {
        X1Y4.fill = GridBagConstraints.BOTH;
        X1Y4.gridx = 1;
        X1Y4.gridy = 4;
        X1Y4.weightx = 1;
        X1Y4.weighty = 1;
    }

    public static final GridBagConstraints X1Y5 = new GridBagConstraints();
    static {
        X1Y5.fill = GridBagConstraints.BOTH;
        X1Y5.gridx = 1;
        X1Y5.gridy = 5;
        X1Y5.weightx = 1;
        X1Y5.weighty = 1;
    }

    public static final GridBagConstraints X1Y6 = new GridBagConstraints();
    static {
        X1Y6.fill = GridBagConstraints.BOTH;
        X1Y6.gridx = 1;
        X1Y6.gridy = 6;
        X1Y6.weightx = 1;
        X1Y6.weighty = 1;
    }

    public static final GridBagConstraints X1Y7 = new GridBagConstraints();
    static {
        X1Y7.fill = GridBagConstraints.BOTH;
        X1Y7.gridx = 1;
        X1Y7.gridy = 7;
        X1Y7.weightx = 1;
        X1Y7.weighty = 1;
    }

    public static final GridBagConstraints X1Y8 = new GridBagConstraints();
    static {
        X1Y8.fill = GridBagConstraints.BOTH;
        X1Y8.gridx = 1;
        X1Y8.gridy = 8;
        X1Y8.weightx = 1;
        X1Y8.weighty = 1;
    }

    public static final GridBagConstraints X1Y9 = new GridBagConstraints();
    static {
        X1Y9.fill = GridBagConstraints.BOTH;
        X1Y9.gridx = 1;
        X1Y9.gridy = 9;
        X1Y9.weightx = 1;
        X1Y9.weighty = 1;
    }

    public static final GridBagConstraints X2Y0 = new GridBagConstraints();
    static {
        X2Y0.fill = GridBagConstraints.BOTH;
        X2Y0.gridx = 2;
        X2Y0.gridy = 0;
        X2Y0.weightx = 1;
        X2Y0.weighty = 1;
    }

    public static final GridBagConstraints X2Y1 = new GridBagConstraints();
    static {
        X2Y1.fill = GridBagConstraints.BOTH;
        X2Y1.gridx = 2;
        X2Y1.gridy = 1;
        X2Y1.weightx = 1;
        X2Y1.weighty = 1;
    }

    public static final GridBagConstraints X2Y2 = new GridBagConstraints();
    static {
        X2Y2.fill = GridBagConstraints.BOTH;
        X2Y2.gridx = 2;
        X2Y2.gridy = 2;
        X2Y2.weightx = 1;
        X2Y2.weighty = 1;
    }

    public static final GridBagConstraints X2Y3 = new GridBagConstraints();
    static {
        X2Y3.fill = GridBagConstraints.BOTH;
        X2Y3.gridx = 2;
        X2Y3.gridy = 3;
        X2Y3.weightx = 1;
        X2Y3.weighty = 1;
    }

    public static final GridBagConstraints X2Y4 = new GridBagConstraints();
    static {
        X2Y4.fill = GridBagConstraints.BOTH;
        X2Y4.gridx = 2;
        X2Y4.gridy = 4;
        X2Y4.weightx = 1;
        X2Y4.weighty = 1;
    }

    public static final GridBagConstraints X2Y5 = new GridBagConstraints();
    static {
        X2Y5.fill = GridBagConstraints.BOTH;
        X2Y5.gridx = 2;
        X2Y5.gridy = 5;
        X2Y5.weightx = 1;
        X2Y5.weighty = 1;
    }

    public static final GridBagConstraints X2Y6 = new GridBagConstraints();
    static {
        X2Y6.fill = GridBagConstraints.BOTH;
        X2Y6.gridx = 2;
        X2Y6.gridy = 6;
        X2Y6.weightx = 1;
        X2Y6.weighty = 1;
    }

    public static final GridBagConstraints X2Y7 = new GridBagConstraints();
    static {
        X2Y7.fill = GridBagConstraints.BOTH;
        X2Y7.gridx = 2;
        X2Y7.gridy = 7;
        X2Y7.weightx = 1;
        X2Y7.weighty = 1;
    }

    public static final GridBagConstraints X2Y8 = new GridBagConstraints();
    static {
        X2Y8.fill = GridBagConstraints.BOTH;
        X2Y8.gridx = 2;
        X2Y8.gridy = 8;
        X2Y8.weightx = 1;
        X2Y8.weighty = 1;
    }

    public static final GridBagConstraints X2Y9 = new GridBagConstraints();
    static {
        X2Y9.fill = GridBagConstraints.BOTH;
        X2Y9.gridx = 2;
        X2Y9.gridy = 9;
        X2Y9.weightx = 1;
        X2Y9.weighty = 1;
    }

    public static final GridBagConstraints X3Y0 = new GridBagConstraints();
    static {
        X3Y0.fill = GridBagConstraints.BOTH;
        X3Y0.gridx = 3;
        X3Y0.gridy = 0;
        X3Y0.weightx = 1;
        X3Y0.weighty = 1;
    }

    public static final GridBagConstraints X3Y1 = new GridBagConstraints();
    static {
        X3Y1.fill = GridBagConstraints.BOTH;
        X3Y1.gridx = 3;
        X3Y1.gridy = 1;
        X3Y1.weightx = 1;
        X3Y1.weighty = 1;
    }

    public static final GridBagConstraints X3Y2 = new GridBagConstraints();
    static {
        X3Y2.fill = GridBagConstraints.BOTH;
        X3Y2.gridx = 3;
        X3Y2.gridy = 2;
        X3Y2.weightx = 1;
        X3Y2.weighty = 1;
    }

    public static final GridBagConstraints X3Y3 = new GridBagConstraints();
    static {
        X3Y3.fill = GridBagConstraints.BOTH;
        X3Y3.gridx = 3;
        X3Y3.gridy = 3;
        X3Y3.weightx = 1;
        X3Y3.weighty = 1;
    }

    public static final GridBagConstraints X3Y4 = new GridBagConstraints();
    static {
        X3Y4.fill = GridBagConstraints.BOTH;
        X3Y4.gridx = 3;
        X3Y4.gridy = 4;
        X3Y4.weightx = 1;
        X3Y4.weighty = 1;
    }

    public static final GridBagConstraints X3Y5 = new GridBagConstraints();
    static {
        X3Y5.fill = GridBagConstraints.BOTH;
        X3Y5.gridx = 3;
        X3Y5.gridy = 5;
        X3Y5.weightx = 1;
        X3Y5.weighty = 1;
    }

    public static final GridBagConstraints X3Y6 = new GridBagConstraints();
    static {
        X3Y6.fill = GridBagConstraints.BOTH;
        X3Y6.gridx = 3;
        X3Y6.gridy = 6;
        X3Y6.weightx = 1;
        X3Y6.weighty = 1;
    }

    public static final GridBagConstraints X3Y7 = new GridBagConstraints();
    static {
        X3Y7.fill = GridBagConstraints.BOTH;
        X3Y7.gridx = 3;
        X3Y7.gridy = 7;
        X3Y7.weightx = 1;
        X3Y7.weighty = 1;
    }

    public static final GridBagConstraints X3Y8 = new GridBagConstraints();
    static {
        X3Y8.fill = GridBagConstraints.BOTH;
        X3Y8.gridx = 3;
        X3Y8.gridy = 8;
        X3Y8.weightx = 1;
        X3Y8.weighty = 1;
    }

    public static final GridBagConstraints X3Y9 = new GridBagConstraints();
    static {
        X3Y9.fill = GridBagConstraints.BOTH;
        X3Y9.gridx = 3;
        X3Y9.gridy = 9;
        X3Y9.weightx = 1;
        X3Y9.weighty = 1;
    }

    public static final GridBagConstraints X4Y0 = new GridBagConstraints();
    static {
        X4Y0.fill = GridBagConstraints.BOTH;
        X4Y0.gridx = 4;
        X4Y0.gridy = 0;
        X4Y0.weightx = 1;
        X4Y0.weighty = 1;
    }

    public static final GridBagConstraints X4Y1 = new GridBagConstraints();
    static {
        X4Y1.fill = GridBagConstraints.BOTH;
        X4Y1.gridx = 4;
        X4Y1.gridy = 1;
        X4Y1.weightx = 1;
        X4Y1.weighty = 1;
    }

    public static final GridBagConstraints X4Y2 = new GridBagConstraints();
    static {
        X4Y2.fill = GridBagConstraints.BOTH;
        X4Y2.gridx = 4;
        X4Y2.gridy = 2;
        X4Y2.weightx = 1;
        X4Y2.weighty = 1;
    }

    public static final GridBagConstraints X4Y3 = new GridBagConstraints();
    static {
        X4Y3.fill = GridBagConstraints.BOTH;
        X4Y3.gridx = 4;
        X4Y3.gridy = 3;
        X4Y3.weightx = 1;
        X4Y3.weighty = 1;
    }

    public static final GridBagConstraints X4Y4 = new GridBagConstraints();
    static {
        X4Y4.fill = GridBagConstraints.BOTH;
        X4Y4.gridx = 4;
        X4Y4.gridy = 4;
        X4Y4.weightx = 1;
        X4Y4.weighty = 1;
    }

    public static final GridBagConstraints X4Y5 = new GridBagConstraints();
    static {
        X4Y5.fill = GridBagConstraints.BOTH;
        X4Y5.gridx = 4;
        X4Y5.gridy = 5;
        X4Y5.weightx = 1;
        X4Y5.weighty = 1;
    }

    public static final GridBagConstraints X4Y6 = new GridBagConstraints();
    static {
        X4Y6.fill = GridBagConstraints.BOTH;
        X4Y6.gridx = 4;
        X4Y6.gridy = 6;
        X4Y6.weightx = 1;
        X4Y6.weighty = 1;
    }

    public static final GridBagConstraints X4Y7 = new GridBagConstraints();
    static {
        X4Y7.fill = GridBagConstraints.BOTH;
        X4Y7.gridx = 4;
        X4Y7.gridy = 7;
        X4Y7.weightx = 1;
        X4Y7.weighty = 1;
    }

    public static final GridBagConstraints X4Y8 = new GridBagConstraints();
    static {
        X4Y8.fill = GridBagConstraints.BOTH;
        X4Y8.gridx = 4;
        X4Y8.gridy = 8;
        X4Y8.weightx = 1;
        X4Y8.weighty = 1;
    }

    public static final GridBagConstraints X4Y9 = new GridBagConstraints();
    static {
        X4Y9.fill = GridBagConstraints.BOTH;
        X4Y9.gridx = 4;
        X4Y9.gridy = 9;
        X4Y9.weightx = 1;
        X4Y9.weighty = 1;
    }

    public static final GridBagConstraints X5Y0 = new GridBagConstraints();
    static {
        X5Y0.fill = GridBagConstraints.BOTH;
        X5Y0.gridx = 5;
        X5Y0.gridy = 0;
        X5Y0.weightx = 1;
        X5Y0.weighty = 1;
    }

    public static final GridBagConstraints X5Y1 = new GridBagConstraints();
    static {
        X5Y1.fill = GridBagConstraints.BOTH;
        X5Y1.gridx = 5;
        X5Y1.gridy = 1;
        X5Y1.weightx = 1;
        X5Y1.weighty = 1;
    }

    public static final GridBagConstraints X5Y2 = new GridBagConstraints();
    static {
        X5Y2.fill = GridBagConstraints.BOTH;
        X5Y2.gridx = 5;
        X5Y2.gridy = 2;
        X5Y2.weightx = 1;
        X5Y2.weighty = 1;
    }

    public static final GridBagConstraints X5Y3 = new GridBagConstraints();
    static {
        X5Y3.fill = GridBagConstraints.BOTH;
        X5Y3.gridx = 5;
        X5Y3.gridy = 3;
        X5Y3.weightx = 1;
        X5Y3.weighty = 1;
    }

    public static final GridBagConstraints X5Y4 = new GridBagConstraints();
    static {
        X5Y4.fill = GridBagConstraints.BOTH;
        X5Y4.gridx = 5;
        X5Y4.gridy = 4;
        X5Y4.weightx = 1;
        X5Y4.weighty = 1;
    }

    public static final GridBagConstraints X5Y5 = new GridBagConstraints();
    static {
        X5Y5.fill = GridBagConstraints.BOTH;
        X5Y5.gridx = 5;
        X5Y5.gridy = 5;
        X5Y5.weightx = 1;
        X5Y5.weighty = 1;
    }

    public static final GridBagConstraints X5Y6 = new GridBagConstraints();
    static {
        X5Y6.fill = GridBagConstraints.BOTH;
        X5Y6.gridx = 5;
        X5Y6.gridy = 6;
        X5Y6.weightx = 1;
        X5Y6.weighty = 1;
    }

    public static final GridBagConstraints X5Y7 = new GridBagConstraints();
    static {
        X5Y7.fill = GridBagConstraints.BOTH;
        X5Y7.gridx = 5;
        X5Y7.gridy = 7;
        X5Y7.weightx = 1;
        X5Y7.weighty = 1;
    }

    public static final GridBagConstraints X5Y8 = new GridBagConstraints();
    static {
        X5Y8.fill = GridBagConstraints.BOTH;
        X5Y8.gridx = 5;
        X5Y8.gridy = 8;
        X5Y8.weightx = 1;
        X5Y8.weighty = 1;
    }

    public static final GridBagConstraints X5Y9 = new GridBagConstraints();
    static {
        X5Y9.fill = GridBagConstraints.BOTH;
        X5Y9.gridx = 5;
        X5Y9.gridy = 9;
        X5Y9.weightx = 1;
        X5Y9.weighty = 1;
    }

    public static final GridBagConstraints X6Y0 = new GridBagConstraints();
    static {
        X6Y0.fill = GridBagConstraints.BOTH;
        X6Y0.gridx = 6;
        X6Y0.gridy = 0;
        X6Y0.weightx = 1;
        X6Y0.weighty = 1;
    }

    public static final GridBagConstraints X6Y1 = new GridBagConstraints();
    static {
        X6Y1.fill = GridBagConstraints.BOTH;
        X6Y1.gridx = 6;
        X6Y1.gridy = 1;
        X6Y1.weightx = 1;
        X6Y1.weighty = 1;
    }

    public static final GridBagConstraints X6Y2 = new GridBagConstraints();
    static {
        X6Y2.fill = GridBagConstraints.BOTH;
        X6Y2.gridx = 6;
        X6Y2.gridy = 2;
        X6Y2.weightx = 1;
        X6Y2.weighty = 1;
    }

    public static final GridBagConstraints X6Y3 = new GridBagConstraints();
    static {
        X6Y3.fill = GridBagConstraints.BOTH;
        X6Y3.gridx = 6;
        X6Y3.gridy = 3;
        X6Y3.weightx = 1;
        X6Y3.weighty = 1;
    }

    public static final GridBagConstraints X6Y4 = new GridBagConstraints();
    static {
        X6Y4.fill = GridBagConstraints.BOTH;
        X6Y4.gridx = 6;
        X6Y4.gridy = 4;
        X6Y4.weightx = 1;
        X6Y4.weighty = 1;
    }

    public static final GridBagConstraints X6Y5 = new GridBagConstraints();
    static {
        X6Y5.fill = GridBagConstraints.BOTH;
        X6Y5.gridx = 6;
        X6Y5.gridy = 5;
        X6Y5.weightx = 1;
        X6Y5.weighty = 1;
    }

    public static final GridBagConstraints X6Y6 = new GridBagConstraints();
    static {
        X6Y6.fill = GridBagConstraints.BOTH;
        X6Y6.gridx = 6;
        X6Y6.gridy = 6;
        X6Y6.weightx = 1;
        X6Y6.weighty = 1;
    }

    public static final GridBagConstraints X6Y7 = new GridBagConstraints();
    static {
        X6Y7.fill = GridBagConstraints.BOTH;
        X6Y7.gridx = 6;
        X6Y7.gridy = 7;
        X6Y7.weightx = 1;
        X6Y7.weighty = 1;
    }

    public static final GridBagConstraints X6Y8 = new GridBagConstraints();
    static {
        X6Y8.fill = GridBagConstraints.BOTH;
        X6Y8.gridx = 6;
        X6Y8.gridy = 8;
        X6Y8.weightx = 1;
        X6Y8.weighty = 1;
    }

    public static final GridBagConstraints X6Y9 = new GridBagConstraints();
    static {
        X6Y9.fill = GridBagConstraints.BOTH;
        X6Y9.gridx = 6;
        X6Y9.gridy = 9;
        X6Y9.weightx = 1;
        X6Y9.weighty = 1;
    }

    public static final GridBagConstraints X7Y0 = new GridBagConstraints();
    static {
        X7Y0.fill = GridBagConstraints.BOTH;
        X7Y0.gridx = 7;
        X7Y0.gridy = 0;
        X7Y0.weightx = 1;
        X7Y0.weighty = 1;
    }

    public static final GridBagConstraints X7Y1 = new GridBagConstraints();
    static {
        X7Y1.fill = GridBagConstraints.BOTH;
        X7Y1.gridx = 7;
        X7Y1.gridy = 1;
        X7Y1.weightx = 1;
        X7Y1.weighty = 1;
    }

    public static final GridBagConstraints X7Y2 = new GridBagConstraints();
    static {
        X7Y2.fill = GridBagConstraints.BOTH;
        X7Y2.gridx = 7;
        X7Y2.gridy = 2;
        X7Y2.weightx = 1;
        X7Y2.weighty = 1;
    }

    public static final GridBagConstraints X7Y3 = new GridBagConstraints();
    static {
        X7Y3.fill = GridBagConstraints.BOTH;
        X7Y3.gridx = 7;
        X7Y3.gridy = 3;
        X7Y3.weightx = 1;
        X7Y3.weighty = 1;
    }

    public static final GridBagConstraints X7Y4 = new GridBagConstraints();
    static {
        X7Y4.fill = GridBagConstraints.BOTH;
        X7Y4.gridx = 7;
        X7Y4.gridy = 4;
        X7Y4.weightx = 1;
        X7Y4.weighty = 1;
    }

    public static final GridBagConstraints X7Y5 = new GridBagConstraints();
    static {
        X7Y5.fill = GridBagConstraints.BOTH;
        X7Y5.gridx = 7;
        X7Y5.gridy = 5;
        X7Y5.weightx = 1;
        X7Y5.weighty = 1;
    }

    public static final GridBagConstraints X7Y6 = new GridBagConstraints();
    static {
        X7Y6.fill = GridBagConstraints.BOTH;
        X7Y6.gridx = 7;
        X7Y6.gridy = 6;
        X7Y6.weightx = 1;
        X7Y6.weighty = 1;
    }

    public static final GridBagConstraints X7Y7 = new GridBagConstraints();
    static {
        X7Y7.fill = GridBagConstraints.BOTH;
        X7Y7.gridx = 7;
        X7Y7.gridy = 7;
        X7Y7.weightx = 1;
        X7Y7.weighty = 1;
    }

    public static final GridBagConstraints X7Y8 = new GridBagConstraints();
    static {
        X7Y8.fill = GridBagConstraints.BOTH;
        X7Y8.gridx = 7;
        X7Y8.gridy = 8;
        X7Y8.weightx = 1;
        X7Y8.weighty = 1;
    }

    public static final GridBagConstraints X7Y9 = new GridBagConstraints();
    static {
        X7Y9.fill = GridBagConstraints.BOTH;
        X7Y9.gridx = 7;
        X7Y9.gridy = 9;
        X7Y9.weightx = 1;
        X7Y9.weighty = 1;
    }

    public static final GridBagConstraints X8Y0 = new GridBagConstraints();
    static {
        X8Y0.fill = GridBagConstraints.BOTH;
        X8Y0.gridx = 8;
        X8Y0.gridy = 0;
        X8Y0.weightx = 1;
        X8Y0.weighty = 1;
    }

    public static final GridBagConstraints X8Y1 = new GridBagConstraints();
    static {
        X8Y1.fill = GridBagConstraints.BOTH;
        X8Y1.gridx = 8;
        X8Y1.gridy = 1;
        X8Y1.weightx = 1;
        X8Y1.weighty = 1;
    }

    public static final GridBagConstraints X8Y2 = new GridBagConstraints();
    static {
        X8Y2.fill = GridBagConstraints.BOTH;
        X8Y2.gridx = 8;
        X8Y2.gridy = 2;
        X8Y2.weightx = 1;
        X8Y2.weighty = 1;
    }

    public static final GridBagConstraints X8Y3 = new GridBagConstraints();
    static {
        X8Y3.fill = GridBagConstraints.BOTH;
        X8Y3.gridx = 8;
        X8Y3.gridy = 3;
        X8Y3.weightx = 1;
        X8Y3.weighty = 1;
    }

    public static final GridBagConstraints X8Y4 = new GridBagConstraints();
    static {
        X8Y4.fill = GridBagConstraints.BOTH;
        X8Y4.gridx = 8;
        X8Y4.gridy = 4;
        X8Y4.weightx = 1;
        X8Y4.weighty = 1;
    }

    public static final GridBagConstraints X8Y5 = new GridBagConstraints();
    static {
        X8Y5.fill = GridBagConstraints.BOTH;
        X8Y5.gridx = 8;
        X8Y5.gridy = 5;
        X8Y5.weightx = 1;
        X8Y5.weighty = 1;
    }

    public static final GridBagConstraints X8Y6 = new GridBagConstraints();
    static {
        X8Y6.fill = GridBagConstraints.BOTH;
        X8Y6.gridx = 8;
        X8Y6.gridy = 6;
        X8Y6.weightx = 1;
        X8Y6.weighty = 1;
    }

    public static final GridBagConstraints X8Y7 = new GridBagConstraints();
    static {
        X8Y7.fill = GridBagConstraints.BOTH;
        X8Y7.gridx = 8;
        X8Y7.gridy = 7;
        X8Y7.weightx = 1;
        X8Y7.weighty = 1;
    }

    public static final GridBagConstraints X8Y8 = new GridBagConstraints();
    static {
        X8Y8.fill = GridBagConstraints.BOTH;
        X8Y8.gridx = 8;
        X8Y8.gridy = 8;
        X8Y8.weightx = 1;
        X8Y8.weighty = 1;
    }

    public static final GridBagConstraints X8Y9 = new GridBagConstraints();
    static {
        X8Y9.fill = GridBagConstraints.BOTH;
        X8Y9.gridx = 8;
        X8Y9.gridy = 9;
        X8Y9.weightx = 1;
        X8Y9.weighty = 1;
    }

    public static final GridBagConstraints X9Y0 = new GridBagConstraints();
    static {
        X9Y0.fill = GridBagConstraints.BOTH;
        X9Y0.gridx = 9;
        X9Y0.gridy = 0;
        X9Y0.weightx = 1;
        X9Y0.weighty = 1;
    }

    public static final GridBagConstraints X9Y1 = new GridBagConstraints();
    static {
        X9Y1.fill = GridBagConstraints.BOTH;
        X9Y1.gridx = 9;
        X9Y1.gridy = 1;
        X9Y1.weightx = 1;
        X9Y1.weighty = 1;
    }

    public static final GridBagConstraints X9Y2 = new GridBagConstraints();
    static {
        X9Y2.fill = GridBagConstraints.BOTH;
        X9Y2.gridx = 9;
        X9Y2.gridy = 2;
        X9Y2.weightx = 1;
        X9Y2.weighty = 1;
    }

    public static final GridBagConstraints X9Y3 = new GridBagConstraints();
    static {
        X9Y3.fill = GridBagConstraints.BOTH;
        X9Y3.gridx = 9;
        X9Y3.gridy = 3;
        X9Y3.weightx = 1;
        X9Y3.weighty = 1;
    }

    public static final GridBagConstraints X9Y4 = new GridBagConstraints();
    static {
        X9Y4.fill = GridBagConstraints.BOTH;
        X9Y4.gridx = 9;
        X9Y4.gridy = 4;
        X9Y4.weightx = 1;
        X9Y4.weighty = 1;
    }

    public static final GridBagConstraints X9Y5 = new GridBagConstraints();
    static {
        X9Y5.fill = GridBagConstraints.BOTH;
        X9Y5.gridx = 9;
        X9Y5.gridy = 5;
        X9Y5.weightx = 1;
        X9Y5.weighty = 1;
    }

    public static final GridBagConstraints X9Y6 = new GridBagConstraints();
    static {
        X9Y6.fill = GridBagConstraints.BOTH;
        X9Y6.gridx = 9;
        X9Y6.gridy = 6;
        X9Y6.weightx = 1;
        X9Y6.weighty = 1;
    }

    public static final GridBagConstraints X9Y7 = new GridBagConstraints();
    static {
        X9Y7.fill = GridBagConstraints.BOTH;
        X9Y7.gridx = 9;
        X9Y7.gridy = 7;
        X9Y7.weightx = 1;
        X9Y7.weighty = 1;
    }

    public static final GridBagConstraints X9Y8 = new GridBagConstraints();
    static {
        X9Y8.fill = GridBagConstraints.BOTH;
        X9Y8.gridx = 9;
        X9Y8.gridy = 8;
        X9Y8.weightx = 1;
        X9Y8.weighty = 1;
    }

    public static final GridBagConstraints X9Y9 = new GridBagConstraints();
    static {
        X9Y9.fill = GridBagConstraints.BOTH;
        X9Y9.gridx = 9;
        X9Y9.gridy = 9;
        X9Y9.weightx = 1;
        X9Y9.weighty = 1;
    }

    // endregion

}
