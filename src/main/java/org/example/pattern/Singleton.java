package org.example.pattern;

public class Singleton {

    /**
     * 饿汉：利用类的静态构造只会被初始化一次来保证单例
     */
    public static class StaticSingleton {
        private static StaticSingleton INSTANCE = new StaticSingleton();

        private StaticSingleton() {
        }

        public static StaticSingleton getInstance() {
            return INSTANCE;
        }
    }

    /**
     * 枚举来实现单例
     */
    public enum EnumSingleton {
        INSTANCE;

        public void test() {
        }

        public static void main(String[] args) {
            EnumSingleton.INSTANCE.test();
        }
    }

    /**
     * 内部类的方式
     */
    public static class LazyStaticInnerSingleton {

        private LazyStaticInnerSingleton() {
        }

        // 只有在访问到类的静态成员、new、反射时才会触发类的初始化，因此是懒加载的
        private static class SingletonHolder {
            private static final LazyStaticInnerSingleton INSTANCE = new LazyStaticInnerSingleton();
        }

        public static LazyStaticInnerSingleton getInstance() {
            return SingletonHolder.INSTANCE;
        }
    }

    /**
     * 锁 + volatile + 双重检查
     */
    public static class LazyDoubleCheckSingleton {

        private static volatile LazyDoubleCheckSingleton instance;

        private LazyDoubleCheckSingleton() {
        }

        public static LazyDoubleCheckSingleton getInstance() {
            if (instance == null) {
                synchronized (LazyDoubleCheckSingleton.class) {
                    if (instance == null) {
                        return instance = new LazyDoubleCheckSingleton(); // 必须使用 volatile
                    }
                }
            }

            return instance;
        }
    }

}
