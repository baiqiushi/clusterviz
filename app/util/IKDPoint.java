package util;

public interface IKDPoint<T extends Number & Comparable<? super T>> {
    public void init(T[] values);

    public T getDimensionValue(int i);

    public void setDimensionValue(int i, T value);

    public boolean equalsTo(IKDPoint y);

    public T distanceTo(IKDPoint y);

    public void increaseSize();

    public boolean leftTo(IKDPoint y);

    public boolean rightTo(IKDPoint y);
}
