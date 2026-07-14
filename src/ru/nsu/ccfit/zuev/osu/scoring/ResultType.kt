package ru.nsu.ccfit.zuev.osu.scoring

enum class ResultType(internal val id: Byte) {
    HIT300(4),
    HIT100(3),
    HIT50(2),
    MISS(1);
}
