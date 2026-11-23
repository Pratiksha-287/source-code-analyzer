
import os
from pathlib import Path
from collections import Counter as C


@staticmethod
def greet(name: str, age: int = 20) -> str:
    if age >= 18:
        return f"Hello {name}"
    return "Minor"


# Module-level variable
APP_NAME = "TaskFlow"


def calculate():
    # Function-level variable
    total = 100
    return total


class Employee:
    # Class-level variable
    department = "Engineering"

    def __init__(self, name):
        self.name = name

    def display(self):
        print(self.name)


for item in [1, 2, 3]:
    print(item)


try:
    greet("Alex")
except ValueError:
    print("Error")

def create_user(
    name: str,
    age: int = 20,
    *args,
    active: bool = True,
    **kwargs
) -> dict:
    pass

def public_function():
    pass


def _private_function():
    pass


class PublicClass:
    pass


_private_variable = 10
public_variable = 20

__all__ = ["greet", "Employee", "public_variable"]

class Animal:
    def eat(self):
        pass


class Dog(Animal):
    def bark(self):
        pass

def validate_user():
    print("Validating user")


def create_user():
    validate_user()
    print("Creating user")


class UserService:
    def save_user(self):
        validate_user()

def outer():
    def inner():
        print("Inside inner")

        def deep_inner():
            print("Inside deep inner")

        deep_inner()

    inner()

import asyncio

async def fetch_user(user_id: int):
    await asyncio.sleep(1)
    return user_id

async def main():
    user = await fetch_user(101)
    print(user)

# Lambda expressions
square = lambda x: x * x
add = lambda a, b: a + b
multiply = lambda x, factor=2: x * factor

numbers = list(map(lambda n: n * 2, [1, 2, 3]))

