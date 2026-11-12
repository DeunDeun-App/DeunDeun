package com.example.deundeun.ui.simulation.pose;

/** 한 프레임의 자세 판정 결과. 위에서부터 먼저 고쳐야 할 순서다. */
public enum PoseCheck {
    /** 사람이 화면에 없거나 어깨가 보이지 않음. 지진은 다리까지 보여야 한다 */
    NO_PERSON,
    /** 지진: 몸을 아직 웅크리지 않음. 화재에서는 쓰지 않는다 */
    NOT_LOW,
    /** 머리(지진) 또는 코와 입(화재)을 가리지 않음 */
    NOT_PROTECTED,
    /** 올바른 대피 자세 */
    CORRECT
}
