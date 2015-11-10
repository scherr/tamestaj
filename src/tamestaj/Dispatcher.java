package tamestaj;

// This class should only be accessible by instrumented and generated code!
// We cannot really guarantee this, but we can at least temporarily make sure that it only becomes
// public at run time.

@SuppressWarnings("unused")
final class Dispatcher {
    private Dispatcher() { }

    public static Object removePersistent(int id) { return Persistor.remove(id); }
    public static int addPersistent(Object object) { return Persistor.add(object); }

    public static <T extends Closure<?>> ClosureHolder<T> makeClosureHolder(boolean isPermanent) {
        return ClosureHolder.make(isPermanent);
    }

    public static Trace traceRecord(Trace trace, int position) { return Trace.record(trace, position); }
    public static <T extends Closure<?>> TraceCache<T> makeTraceCache(int maxSize) { return TraceCache.make(maxSize); }
    public static <T extends Closure<?>> ClosureHolder<T> getTraceCachedClosureHolder(TraceCache<T> cache, Trace trace) {
        return cache.getCachedClosureHolder(trace);
    }

    public static <V> Expression.Value<V> liftObject(V value) { return Expression.ObjectValue.make(value); }
    public static Expression.Value<Boolean> liftBoolean(boolean value) { return Expression.BooleanValue.make(value); }
    public static Expression.Value<Integer> liftInteger(int value) { return Expression.IntegerValue.make(value); }
    public static Expression.Value<Long> liftLong(long value) { return Expression.LongValue.make(value); }
    public static Expression.Value<Float> liftFloat(float value) { return Expression.FloatValue.make(value); }
    public static Expression.Value<Double> liftDouble(double value) { return Expression.DoubleValue.make(value); }
    public static Expression.Value<Byte> liftByte(byte value) { return Expression.ByteValue.make(value); }
    public static Expression.Value<Character> liftCharacter(char value) { return Expression.CharacterValue.make(value); }
    public static Expression.Value<Short> liftShort(short value) { return Expression.ShortValue.make(value); }

    public static Expression.Value makeConstant(Expression expression) { return ((Expression.Value) expression).makeConstant();  }

    public static <V> Expression.Value<V> liftConstantObject(V value) { return Expression.ObjectValue.makeConstant(value); }
    public static Expression.Value<Boolean> liftConstantBoolean(boolean value) { return Expression.BooleanValue.makeConstant(value); }
    public static Expression.Value<Integer> liftConstantInteger(int value) { return Expression.IntegerValue.makeConstant(value); }
    public static Expression.Value<Long> liftConstantLong(long value) { return Expression.LongValue.makeConstant(value); }
    public static Expression.Value<Float> liftConstantFloat(float value) { return Expression.FloatValue.makeConstant(value); }
    public static Expression.Value<Double> liftConstantDouble(double value) { return Expression.DoubleValue.makeConstant(value); }
    public static Expression.Value<Byte> liftConstantByte(byte value) { return Expression.ByteValue.makeConstant(value); }
    public static Expression.Value<Character> liftConstantCharacter(char value) { return Expression.CharacterValue.makeConstant(value); }
    public static Expression.Value<Short> liftConstantShort(short value) { return Expression.ShortValue.makeConstant(value); }

    public static <V> V materializeAsObject(Expression expression) { return (V) expression.materializeAsObject(); }
    public static boolean materializeAsBoolean(Expression expression) { return expression.materializeAsBoolean(); }
    public static int materializeAsInteger(Expression expression) { return expression.materializeAsInteger(); }
    public static long materializeAsLong(Expression expression) { return expression.materializeAsLong(); }
    public static float materializeAsFloat(Expression expression) { return expression.materializeAsFloat(); }
    public static double materializeAsDouble(Expression expression) { return expression.materializeAsDouble(); }
    public static byte materializeAsByte(Expression expression) { return expression.materializeAsByte(); }
    public static char materializeAsCharacter(Expression expression) { return expression.materializeAsCharacter(); }
    public static short materializeAsShort(Expression expression) {
        return expression.materializeAsShort();
    }

    public static Expression convertToBoolean(Expression expression) { return expression.convertToBoolean(); }
    public static Expression convertToInteger(Expression expression) { return expression.convertToInteger(); }
    public static Expression convertToLong(Expression expression) { return expression.convertToLong(); }
    public static Expression convertToFloat(Expression expression) { return expression.convertToFloat(); }
    public static Expression convertToDouble(Expression expression) { return expression.convertToDouble(); }
    public static Expression convertToByte(Expression expression) { return expression.convertToByte(); }
    public static Expression convertToCharacter(Expression expression) { return expression.convertToCharacter(); }
    public static Expression convertToShort(Expression expression) { return expression.convertToShort(); }

    public static <V> LocalCarrier liftObjectToLocalCarrier(V value) {
        return new LocalCarrier(Expression.ObjectValue.make(value));
    }
    public static <V> LocalCarrier liftConstantObjectToLocalCarrier(V value) {
        return new LocalCarrier(Expression.ObjectValue.makeConstant(value));
    }
    public static <V> GlobalCarrier liftObjectToGlobalCarrier(V value) {
        return new GlobalCarrier(Expression.ObjectValue.make(value));
    }
    public static <V> GlobalCarrier liftConstantObjectToGlobalCarrier(V value) {
        return new GlobalCarrier(Expression.ObjectValue.makeConstant(value));
    }

    public static LocalCarrier makeConstantLocalCarrierChecked(LocalCarrier carrier) {
        Expression payload = unloadLocalCarrier(carrier);
        if (payload != null) {
            if (payload instanceof Expression.Value) {
                return new LocalCarrier(((Expression.Value) payload).makeConstant());
            } else {
                return carrier;
            }
        } else {
            return liftConstantObjectToLocalCarrier(carrier);
        }
    }
    public static GlobalCarrier makeConstantGlobalCarrierChecked(GlobalCarrier carrier) {
        Expression payload = unloadGlobalCarrier(carrier);
        if (payload != null) {
            if (payload instanceof Expression.Value) {
                return new GlobalCarrier(((Expression.Value) payload).makeConstant());
            } else {
                return carrier;
            }
        } else {
            return liftConstantObjectToGlobalCarrier(carrier);
        }
    }
    public static Object makeConstantMaybeCarrierChecked(Object object) {
        if (object instanceof LocalCarrier) {
            return makeConstantLocalCarrierChecked((LocalCarrier) object);
        } else if (object instanceof GlobalCarrier) {
            return makeConstantGlobalCarrierChecked((GlobalCarrier) object);
        } else {
            return new LocalCarrier(Expression.ObjectValue.makeConstant(object));
        }
    }

    public static Expression selfLiftGlobalCarrier(GlobalCarrier carrier) {
        synchronized (carrier) {
            if (carrier.self == null) {
                carrier.self = liftObject(carrier);
            }
        }
        return carrier.self;
    }

    public static Expression unloadGlobalCarrier(GlobalCarrier carrier) {
        return carrier.payload;
    }
    public static Expression unloadGlobalCarrierChecked(GlobalCarrier carrier) {
        if (carrier == null) {
            return Expression.ObjectValue.make(null);
        } else {
            if (carrier.payload == null) {
                return selfLiftGlobalCarrier(carrier);
            } else {
                return carrier.payload;
            }
        }
    }
    public static Expression unloadLocalCarrier(LocalCarrier carrier) { return carrier.payload; }
    public static Expression unloadLocalCarrierChecked(LocalCarrier carrier) {
        if (carrier == null) {
            return Expression.ObjectValue.make(null);
        } else {
            if (carrier.payload == null) {
                synchronized (carrier) {
                    if (carrier.self == null) {
                        carrier.self = liftObject(carrier);
                    }
                }
                return carrier.self;
            } else {
                return carrier.payload;
            }
        }
    }
    public static Expression unloadMaybeLocalCarrierChecked(Object object) {
        if (object instanceof LocalCarrier) {
            return unloadLocalCarrierChecked((LocalCarrier) object);
        } else {
            return Expression.ObjectValue.make(object);
        }
    }
    public static Expression unloadMaybeGlobalCarrierChecked(Object object) {
        if (object instanceof GlobalCarrier) {
            return unloadGlobalCarrierChecked((GlobalCarrier) object);
        } else {
            return Expression.ObjectValue.make(object);
        }
    }
    public static Expression unloadCarrier(Object object) {
        if (object instanceof LocalCarrier) {
            return unloadLocalCarrier((LocalCarrier) object);
        } else if (object instanceof GlobalCarrier) {
            return unloadGlobalCarrier((GlobalCarrier) object);
        } else {
            throw new UnsupportedOperationException();
        }
    }
    public static Expression unloadMaybeCarrierChecked(Object object) {
        if (object instanceof LocalCarrier) {
            return unloadLocalCarrierChecked((LocalCarrier) object);
        } else if (object instanceof GlobalCarrier) {
            return unloadGlobalCarrierChecked((GlobalCarrier) object);
        } else {
            return Expression.ObjectValue.make(object);
        }
    }

    public static Object materializeLocalCarrier(LocalCarrier carrier) {
        return materializeAsObject(carrier.payload);
    }
    public static Object materializeLocalCarrierChecked(LocalCarrier carrier) {
        if (carrier == null || carrier.payload == null) {
            return carrier;
        } else {
            return materializeAsObject(carrier.payload);
        }
    }
    public static Object materializeMaybeLocalCarrierChecked(Object object) {
        if (object instanceof LocalCarrier) {
            return materializeLocalCarrierChecked((LocalCarrier) object);
        } else {
            return object;
        }
    }
}
