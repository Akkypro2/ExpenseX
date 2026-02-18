from sqlalchemy import Column, Integer, String, Float, ForeignKey
from sqlalchemy.orm import relationship
from database import Base

class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    email = Column(String, unique=True, index=True)
    hashed_password = Column(String)

    expenses = relationship("Expense", back_populates="owner")

class Expense(Base):
    __tablename__ = "expense"

    id = Column(Integer, primary_key=True, index=True)
    merchant = Column(String, index=True)
    amount = Column(Float)
    date = Column(String)
    category = Column(String)
    type = Column(String, default = 'Debit')

    user_id = Column(Integer, ForeignKey("users.id"))
    owner = relationship("User", back_populates="expenses")
    