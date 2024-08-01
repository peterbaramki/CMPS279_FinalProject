from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import requests

app = FastAPI()

API_URL = "https://api-inference.huggingface.co/models/finiteautomata/bertweet-base-sentiment-analysis"
headers = {"Authorization": "Bearer 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"}

class SentimentRequest(BaseModel):
    text: str

def query(payload):
    response = requests.post(API_URL, headers=headers, json=payload)
    if response.status_code == 200:
        return response.json()
    else:
        raise HTTPException(status_code=response.status_code, detail="Error in Hugging Face API request")

@app.post("/analyze")
def analyze_sentiment(request: SentimentRequest):
    result = query({"inputs": request.text})
    if isinstance(result, list) and len(result) == 1 and isinstance(result[0], list):
        result = result[0]
    return result


@app.get("/")
def read_root():
    return {"message": "API is running."}
