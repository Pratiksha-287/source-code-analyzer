
from .utils import helper
from ..shared import *
from package.subpackage import item as renamed_item


def traced(func):
    return func


@traced
@staticmethod
def calculate(value: int, factor: float = 2.0) -> float:
    local_value: int = value * 2

    def nested(inner: str) -> str:
        nested_value: str = inner
        return nested_value

    return local_value * factor


values = [number * 2 for number in numbers if number > 0]

lookup = {
    number: number * number
    for number in numbers
}

unique = {
    number * 2
    for number in numbers
}

stream = (
    number * 2
    for number in numbers
)

nested_values = [
    x + y
    for x in rows
    for y in columns
    if y > 1
]
