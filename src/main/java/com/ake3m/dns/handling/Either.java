package com.ake3m.dns.handling;

import java.util.function.Function;

public sealed interface Either<E, S> {

    static <E, S> Either<E, S> left(E error, S data) {
        return new Left<>(error, data);
    }

    static <E, S> Either<E, S> right(S data) {
        return new Right<>(data);
    }

    default <E2> Either<E2, S> mapLeft(Function<? super E, ? extends E2> mapper) {
        if (this instanceof Left<E, S>(E error, S data)) {
            return new Left<>(mapper.apply(error), data);
        }
        Right<E, S> r = (Right<E, S>) this;
        return new Right<>(r.data);
    }

    default <S2> Either<E, S2> mapRight(Function<? super S, ? extends S2> mapper) {
        if (this instanceof Right<E, S>(S data)) {
            return new Right<>(mapper.apply(data));
        }
        Left<E, S> l = (Left<E, S>) this;
        return new Left<>(l.error, mapper.apply(l.data));
    }

    record Left<E, S>(E error, S data) implements Either<E, S> {
    }

    record Right<E, S>(S data) implements Either<E, S> {
    }
}
