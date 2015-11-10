package tamestaj;

public class GlobalCarrier {
    protected GlobalCarrier() {
        payload = null;
    }

    GlobalCarrier(Expression payload) {
        this.payload = payload;
    }

    final Expression payload;

    // This is (on-demand) relevant to maintain identity when self-lifting...
    volatile Expression.Value<?> self;
}
