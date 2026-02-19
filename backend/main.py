from fastapi import FastAPI, UploadFile, File, HTTPException, Depends, status
from fastapi.security import OAuth2PasswordBearer, OAuth2PasswordRequestForm
from pydantic import BaseModel
from sqlalchemy.orm import Session
from sqlalchemy import desc
import google.generativeai as genai
import os
from dotenv import load_dotenv
from PIL import Image
import io
import json
from typing import List, Optional
from datetime import datetime, timedelta
from passlib.context import CryptContext
from jose import JWTError, jwt
import firebase_admin
from firebase_admin import auth, credentials

if not firebase_admin._apps:
    cred = credentials.Certificate("serviceAccountKey.json")
    firebase_admin.initialize_app(cred)

# Import database files
import models
from database import engine, get_db

# Create tables
models.Base.metadata.create_all(bind=engine)

load_dotenv()
genai.configure(api_key=os.getenv("GEMINI_API_KEY"))

# --- SECURITY CONFIG ---
SECRET_KEY = "YOUR_SUPER_SECRET_KEY_HERE" # Change this!
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 30000 # Long expiry for mobile app convenience

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="token")

app = FastAPI()

# --- Data Models ---
class GoogleLoginRequest(BaseModel):
    token: str

class UserCreate(BaseModel):
    email: str
    password: str

class Token(BaseModel):
    access_token: str
    token_type: str

class ExpenseCreate(BaseModel):
    merchant: str
    amount: float
    date: str
    category: str
    type: str = "Debit"

class Message(BaseModel):
    text: str
    isUser: bool

class ChatRequest(BaseModel):
    message: str
    history: List[Message] = []

# --- Helper Functions ---
def verify_password(plain_password, hashed_password):
    return pwd_context.verify(plain_password, hashed_password)

def get_password_hash(password):
    return pwd_context.hash(password)

def create_access_token(data: dict, expires_delta: Optional[timedelta] = None):
    to_encode = data.copy()
    if expires_delta:
        expire = datetime.utcnow() + expires_delta
    else:
        expire = datetime.utcnow() + timedelta(minutes=15)
    to_encode.update({"exp": expire})
    encoded_jwt = jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)
    return encoded_jwt

async def get_current_user(token: str = Depends(oauth2_scheme), db: Session = Depends(get_db)):
    credentials_exception = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Could not validate credentials",
        headers={"WWW-Authenticate": "Bearer"},
    )
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        email: str = payload.get("sub")
        if email is None:
            raise credentials_exception
    except JWTError:
        raise credentials_exception
    
    user = db.query(models.User).filter(models.User.email == email).first()
    if user is None:
        raise credentials_exception
    return user

# --- AUTH ENDPOINTS ---

@app.post("/google-login", response_model=Token)
def google_login(request: GoogleLoginRequest, db: Session = Depends(get_db)):
    try:
        # 1. Verify the token with Google/Firebase
        decoded_token = auth.verify_id_token(request.token)
        email = decoded_token.get('email')
        
        if not email:
            raise HTTPException(status_code=400, detail="Invalid Google Token (No Email)")

        # 2. Check if user exists in YOUR database
        user = db.query(models.User).filter(models.User.email == email).first()
        
        if not user:
            # 3. If new user, register them automatically
            # We set a dummy password since they log in via Google
            user = models.User(email=email, hashed_password="GOOGLE_OAUTH_USER")
            db.add(user)
            db.commit()
            db.refresh(user)

        # 4. Generate YOUR app's JWT token (same as normal login)
        access_token_expires = timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
        access_token = create_access_token(
            data={"sub": user.email}, expires_delta=access_token_expires
        )
        return {"access_token": access_token, "token_type": "bearer"}

    except Exception as e:
        print(f"Google Login Error: {e}")
        raise HTTPException(status_code=401, detail="Google Authentication Failed")

@app.post("/register", response_model=Token)
def register_user(user: UserCreate, db: Session = Depends(get_db)):
    db_user = db.query(models.User).filter(models.User.email == user.email).first()
    if db_user:
        raise HTTPException(status_code=400, detail="Email already registered")
    
    hashed_password = get_password_hash(user.password)
    new_user = models.User(email=user.email, hashed_password=hashed_password)
    db.add(new_user)
    db.commit()
    db.refresh(new_user)
    
    access_token = create_access_token(data={"sub": new_user.email})
    return {"access_token": access_token, "token_type": "bearer"}

@app.post("/token", response_model=Token)
def login_for_access_token(form_data: OAuth2PasswordRequestForm = Depends(), db: Session = Depends(get_db)):
    user = db.query(models.User).filter(models.User.email == form_data.username).first()
    if not user or not verify_password(form_data.password, user.hashed_password):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect username or password",
            headers={"WWW-Authenticate": "Bearer"},
        )
    access_token_expires = timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    access_token = create_access_token(
        data={"sub": user.email}, expires_delta=access_token_expires
    )
    return {"access_token": access_token, "token_type": "bearer"}

# --- PROTECTED ENDPOINTS ---

@app.post("/save-expense")
def create_expense(expense: ExpenseCreate, db: Session = Depends(get_db), current_user: models.User = Depends(get_current_user)):
    try:
        db_expense = models.Expense(
            merchant=expense.merchant,
            amount=expense.amount,
            date=expense.date,
            category=expense.category,
            type=expense.type,
            user_id=current_user.id # Link to User
        )
        
        db.add(db_expense)
        db.commit()      
        db.refresh(db_expense)
        
        return {"status": "saved", "id": db_expense.id}

    except Exception as e:
        print(f"Error: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/expenses")
def get_expenses(db: Session = Depends(get_db), current_user: models.User = Depends(get_current_user)):
    # Only fetch expenses belonging to this user
    return db.query(models.Expense).filter(models.Expense.user_id == current_user.id).order_by(desc(models.Expense.id)).all()

@app.post("/chat")
async def chat_with_ai(request: ChatRequest, db: Session = Depends(get_db), current_user: models.User = Depends(get_current_user)):
    try:
        # Filter context by user
        expenses = db.query(models.Expense).filter(models.Expense.user_id == current_user.id).order_by(desc(models.Expense.id)).all()
        expense_context = "Here is my recent spending history:\n"
        for e in expenses:
            expense_context += f"- {e.date}: {e.merchant} cost {e.amount} ({e.category})\n"

        system_instruction = f"""
        You are a smart financial assistant. 
        1. Use the 'Database' below to answer questions about spending.
        2. If the user asks a follow-up question (like 'and who was the merchant?'), refer to the previous chat history.
        3. Be concise and friendly
        {expense_context}
        """
        gemini_history = []
        gemini_history.append({"role": "user", "parts": [system_instruction]})
        gemini_history.append({"role": "model", "parts": ["Understood. I will answer based on your spending history."]})

        for msg in request.history:
            role = "user" if msg.isUser else "model"
            gemini_history.append({"role": role, "parts": [msg.text]})

        model = genai.GenerativeModel('gemini-2.5-flash') # Updated model name
        chat = model.start_chat(history=gemini_history)
        response = chat.send_message(request.message)
        
        return {"reply": response.text}
    except Exception as e:
        print(f"Chat Error: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/analyze-receipt")
async def analyze_receipt(file: UploadFile = File(...), db: Session = Depends(get_db), current_user: models.User = Depends(get_current_user)):
    try:
        contents = await file.read()
        image = Image.open(io.BytesIO(contents))

        prompt = """
        Analyze this receipt. Return ONLY a raw JSON object (no markdown, no backticks).
        Extract these fields:
        - merchant (string): Store name
        - date (string): DD MM YYYY format
        - amount (string): Total amount (just number, no currency symbol)
        - category (string): Choose from [Food, Travel, Entertainment, Grocery, Shopping, Bills, Other]
        """
        model = genai.GenerativeModel('gemini-2.5-flash')
        response = model.generate_content([prompt, image])
        clean_text = response.text.replace("```json", "").replace("```", "").strip()
        data = json.loads(clean_text)

        db_expense = models.Expense(
            merchant=data["merchant"],
            amount=data["amount"],
            date=data["date"],
            category=data["category"],
            type="Debit",
            user_id=current_user.id # Link to User
        )

        db.add(db_expense)
        db.commit()
        db.refresh(db_expense)

        return {
            "id": db_expense.id,
            "merchant": db_expense.merchant,
            "amount": db_expense.amount,
            "date": db_expense.date,
            "category": db_expense.category
        }

    except Exception as e:
        print(f"Error: {e}")
        raise HTTPException(status_code=500, detail="Failed")