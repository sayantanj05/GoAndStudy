"""Production middleware: Rate limiting + Caching."""
import time
from typing import Callable
from collections import defaultdict, deque
from functools import wraps
from fastapi import Request, HTTPException, status

# Redis or in-memory cache
CACHE_TTL = 300  # 5 minutes
RATE_LIMIT_WINDOW = 60  # 1 minute
RATE_LIMIT_MAX = 100  # requests per minute per IP

class RateLimiter:
    def __init__(self):
        self.requests = defaultdict(deque)
    
    def is_allowed(self, client_ip: str) -> bool:
        now = time.time()
        # Clean old requests
        self.requests[client_ip] = deque([
            t for t in self.requests[client_ip] 
            if now - t < RATE_LIMIT_WINDOW
        ])
        
        if len(self.requests[client_ip]) < RATE_LIMIT_MAX:
            self.requests[client_ip].append(now)
            return True
        return False

rate_limiter = RateLimiter()

def rate_limit():
    def decorator(f: Callable):
        @wraps(f)
        async def decorated_function(request: Request, *args, **kwargs):
            client_ip = request.client.host
            if not rate_limiter.is_allowed(client_ip):
                raise HTTPException(
                    status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                    detail="Rate limit exceeded. Try again in 1 minute."
                )
            return await f(request, *args, **kwargs)
        return decorated_function
    return decorator

def cache_response(ttl: int = CACHE_TTL):
    cache = {}
    def decorator(f: Callable):
        @wraps(f)
        async def decorated_function(*args, **kwargs):
            cache_key = f"{f.__name__}:{hash(str(args) + str(kwargs))}"
            if cache_key in cache and time.time() - cache[cache_key]['time'] < ttl:
                return cache[cache_key]['data']
            result = await f(*args, **kwargs)
            cache[cache_key] = {'data': result, 'time': time.time()}
            return result
        return decorated_function
    return decorator

