package com.sally.imagepicker.bean;

/**
 * Created by sally on 16/3/30.
 */
public class FloderBean {

    private String currentDir;
    private String name;
    private String firstImgPath;
    private int count;

    public FloderBean() {
    }

    public String getCurrentDir() {
        return currentDir;
    }

    public void setCurrentDir(String currentDir) {
        this.currentDir = currentDir;
        int index = this.currentDir.lastIndexOf("/");
        this.name = this.currentDir.substring(index + 1);
    }

    public String getName() {
        return name;
    }

    public String getFirstImgPath() {
        return firstImgPath;
    }

    public void setFirstImgPath(String firstImgPath) {
        this.firstImgPath = firstImgPath;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
