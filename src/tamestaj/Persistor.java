package tamestaj;

import java.util.ArrayList;

final class Persistor {
    private static int firstFreeId = 0;
    private static final ArrayList<Object> list = new ArrayList<>();

    private Persistor() { }

    static Object get(int id) {
        synchronized (list) {
            if (id >= list.size()) {
                return null;
            } else {
                return list.get(id);
            }
        }
    }

    static Object remove(int id) {
        synchronized (list) {
            Object object = get(id);
            list.set(id, null);
            if (firstFreeId > id) {
                firstFreeId = id;
            }
            return object;
        }
    }

    static int add(Object object) {
        if (object == null) {
            throw new NullPointerException();
        }
        synchronized (list) {
            int id = firstFreeId;
            if (firstFreeId >= list.size()) {
                list.add(object);
                firstFreeId++;
            } else {
                list.set(id, object);
                int newFirstFreeId = id + 1;
                while (newFirstFreeId < list.size()) {
                    if (list.get(newFirstFreeId) == null) {
                        break;
                    }

                    newFirstFreeId++;
                }
                firstFreeId = newFirstFreeId;
            }

            return id;
        }
    }
}
