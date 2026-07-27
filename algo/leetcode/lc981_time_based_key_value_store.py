"""LeetCode 981: binary-searchable repair decision history."""

from bisect import bisect_right


class TimeMap:
    def __init__(self) -> None:
        self._values: dict[str, list[tuple[int, str]]] = {}

    def set(self, key: str, value: str, timestamp: int) -> None:
        history = self._values.setdefault(key, [])
        if history and timestamp <= history[-1][0]:
            raise ValueError("timestamps must increase for each key")
        history.append((timestamp, value))

    def get(self, key: str, timestamp: int) -> str:
        history = self._values.get(key, [])
        index = bisect_right(history, (timestamp, chr(0x10FFFF))) - 1
        return history[index][1] if index >= 0 else ""


def _self_test() -> None:
    store = TimeMap()
    checks = [
        ("missing key", lambda: store.get("missing", 1), ""),
    ]
    store.set("repair-1", "MANUAL_REVIEW", 2)
    store.set("repair-1", "RUNNER_UP", 5)
    store.set("repair-2", "", 3)
    checks.extend([
        ("before first", lambda: store.get("repair-1", 1), ""),
        ("exact hit", lambda: store.get("repair-1", 2), "MANUAL_REVIEW"),
        ("between versions", lambda: store.get("repair-1", 4), "MANUAL_REVIEW"),
        ("late query", lambda: store.get("repair-1", 99), "RUNNER_UP"),
        ("multiple versions", lambda: store.get("repair-1", 5), "RUNNER_UP"),
        ("isolated key", lambda: store.get("repair-2", 2), ""),
        ("empty value", lambda: store.get("repair-2", 3), ""),
    ])
    for index, (name, operation, expected) in enumerate(checks, 1):
        actual = operation()
        assert actual == expected, (name, actual, expected)
        print(f"PASS case_{index} {name}: {actual!r}")
    try:
        store.set("repair-1", "bad", 5)
        raise AssertionError("non-increasing timestamp was accepted")
    except ValueError:
        print("PASS case_9 non-increasing timestamp rejected")
    print("PASS 9 tests")


if __name__ == "__main__":
    _self_test()
