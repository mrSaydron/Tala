from transformers import GPTJForCausalLM, GPT2Tokenizer

# Скачиваем модель и токенизатор
model = GPTJForCausalLM.from_pretrained("EleutherAI/gpt-j-6B")
tokenizer = GPT2Tokenizer.from_pretrained("EleutherAI/gpt-j-6B")

# Сохраняем модель и токенизатор
model.save_pretrained("gptj_model")
tokenizer.save_pretrained("gptj_model")
