package tamestaj;

public class LocalCarrier {
    protected LocalCarrier() {
        payload = null;
    }

    LocalCarrier(Expression payload) {
        this.payload = payload;
    }

    final Expression payload;

    // This is (on-demand) relevant to maintain identity when carrier unloading forces self-lifting...
    volatile Expression.Value<?> self;
}
