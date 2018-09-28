export class CounterExample {
    explanation?: string;
    steps?: Array<string>;
}

export class ValidationInfo {
    valid = true;
    counterExamples?: Array<CounterExample>;
}
