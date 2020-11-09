package net.allape.utils;

public final class LinuxPermissions {

    /**
     * 转换linux文件权限为rwx的格式
     * @param permissions 权限
     * @return 人类可读的权限
     */
    public static String humanReadable (int permissions) {
        String[] numbers = Integer.toOctalString(permissions & 0xfff).split("");
        StringBuilder builder = new StringBuilder(numbers.length * 3);
        for (String number : numbers) {
            switch (number) {
                case "0": builder.append("---"); break;
                case "1": builder.append("--x"); break;
                case "2": builder.append("-w-"); break;
                case "3": builder.append("-wx"); break;
                case "4": builder.append("r--"); break;
                case "5": builder.append("r-x"); break;
                case "6": builder.append("rw-"); break;
                case "7": builder.append("rwx"); break;
            }
        }
        return builder.toString();
    }

}
