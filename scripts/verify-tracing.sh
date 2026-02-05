#!/bin/bash

# 🕵️ Tracing Verification Script
# 
# This script verifies that distributed tracing is working correctly
# by making test requests and checking if traces appear in Jaeger.
# 
# Note: This script requires curl to make API calls.
# Install curl or use wait-for-services.sh for health checking without curl.

set -e

JAEGER_URL="http://localhost:16686"
API_GATEWAY="http://localhost:8080"
AUTH_SERVER="http://localhost:8081"

echo "🕵️  Leaky Tokens Tracing Verification"
echo "======================================"
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if curl is available
if ! command -v curl &> /dev/null; then
    echo -e "${YELLOW}⚠ Warning: curl is not installed${NC}"
    echo "   This script requires curl to make API calls."
    echo "   Install curl or use wait-for-services.sh for basic health checks."
    echo ""
fi

# Function to check if a URL is accessible using bash /dev/tcp
check_url_bash() {
    local url=$1
    local host
    local port
    
    # Parse URL to get host and port
    if [[ $url =~ http://([^:/]+)(:([0-9]+))?(/.*)? ]]; then
        host="${BASH_REMATCH[1]}"
        port="${BASH_REMATCH[3]:-80}"
        
        # Use bash built-in /dev/tcp to check connectivity
        if timeout 2 bash -c "exec 3<>/dev/tcp/$host/$port" 2>/dev/null; then
            return 0
        fi
    fi
    return 1
}

# Check if Jaeger is running
echo "🔍 Checking Jaeger..."
if check_url_bash "${JAEGER_URL}"; then
    echo -e "${GREEN}✓ Jaeger UI is accessible at ${JAEGER_URL}${NC}"
else
    echo -e "${RED}✗ Jaeger is not running${NC}"
    echo "   Start it with: docker-compose -f docker-compose.infra.yml up -d"
    echo "   Or: docker-compose -f docker-compose.full.yml up -d"
    exit 1
fi

# Check if curl is available for API calls
if ! command -v curl &> /dev/null; then
    echo ""
    echo -e "${YELLOW}⚠ Cannot proceed without curl${NC}"
    echo "   To verify tracing:"
    echo "   1. Install curl"
    echo "   2. Make manual API calls to generate traces"
    echo "   3. Check Jaeger UI at http://localhost:16686"
    echo ""
    echo "   Jaeger UI: ${JAEGER_URL}"
    exit 0
fi

# Check services
echo ""
echo "🔍 Checking services..."
for service in api-gateway auth-server token-service analytics-service; do
    if curl -s "${API_GATEWAY}/actuator/health" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ ${service} is up${NC}"
        break
    fi
done

# Register a test user
echo ""
echo "👤 Creating test user..."
REGISTER_RESPONSE=$(curl -s -X POST "${AUTH_SERVER}/api/v1/auth/register" \
    -H "Content-Type: application/json" \
    -d '{"username":"trace-test-'$(date +%s)'","email":"trace@test.com","password":"password123"}')

USER_ID=$(echo "$REGISTER_RESPONSE" | jq -r '.userId')
TOKEN=$(echo "$REGISTER_RESPONSE" | jq -r '.token')

if [ "$USER_ID" == "null" ] || [ -z "$USER_ID" ]; then
    echo -e "${YELLOW}⚠ Could not create user, trying login...${NC}"
    LOGIN_RESPONSE=$(curl -s -X POST "${AUTH_SERVER}/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username":"trace-test","password":"password123"}')
    TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.token')
    USER_ID="00000000-0000-0000-0000-000000000001"
else
    echo -e "${GREEN}✓ User created: ${USER_ID}${NC}"
fi

if [ "$TOKEN" == "null" ] || [ -z "$TOKEN" ]; then
    echo -e "${RED}✗ Could not obtain JWT token${NC}"
    exit 1
fi

# Generate traces
echo ""
echo "📡 Generating test traces..."
echo "   Making API calls to create distributed traces..."

# Call 1: Check quota
curl -s -H "Authorization: Bearer ${TOKEN}" \
    "${API_GATEWAY}/api/v1/tokens/quota?userId=${USER_ID}&provider=openai" > /dev/null
echo "   ✓ Quota check request sent"

# Call 2: Consume tokens (this creates a full distributed trace)
curl -s -X POST "${API_GATEWAY}/api/v1/tokens/consume" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${TOKEN}" \
    -d "{\"userId\":\"${USER_ID}\",\"provider\":\"openai\",\"tokens\":50,\"prompt\":\"Test trace\"}" > /dev/null
echo "   ✓ Token consumption request sent"

# Call 3: Analytics
curl -s "${API_GATEWAY}/api/v1/analytics/usage?provider=openai&limit=10" \
    -H "Authorization: Bearer ${TOKEN}" > /dev/null
echo "   ✓ Analytics query request sent"

# Wait for traces to be processed
echo ""
echo "⏳ Waiting for traces to be processed (5 seconds)..."
sleep 5

# Check for traces in Jaeger
echo ""
echo "🔍 Checking for traces in Jaeger..."

# Search for recent traces
SERVICES=("api-gateway" "token-service" "auth-server" "analytics-service")
TRACE_FOUND=false

for service in "${SERVICES[@]}"; do
    # Query Jaeger API for traces
    TRACES=$(curl -s "${JAEGER_URL}/api/traces?service=${service}&limit=1" 2>/dev/null || echo "{}")
    
    if echo "$TRACES" | jq -e '.data | length > 0' > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Found traces for ${service}${NC}"
        TRACE_FOUND=true
    fi
done

echo ""
if [ "$TRACE_FOUND" = true ]; then
    echo -e "${GREEN}✅ SUCCESS! Distributed tracing is working correctly!${NC}"
    echo ""
    echo "📊 View your traces at:"
    echo "   ${JAEGER_URL}"
    echo ""
    echo "💡 Tips:"
    echo "   - Select a service from the dropdown"
    echo "   - Click 'Find Traces' to see recent requests"
    echo "   - Click on any trace to see the detailed call graph"
    echo ""
    echo "📚 Documentation:"
    echo "   docs/TRACING.md"
else
    echo -e "${YELLOW}⚠ No traces found yet${NC}"
    echo "   This might be because:"
    echo "   - Traces haven't been indexed yet (wait a few more seconds)"
    echo "   - Services are not configured with tracing enabled"
    echo "   - Sampling rate is set to 0"
    echo ""
    echo "🔧 To fix:"
    echo "   1. Ensure SPRING_PROFILES_ACTIVE includes 'tracing'"
    echo "   2. Check MANAGEMENT_TRACING_ENABLED=true"
    echo "   3. Verify MANAGEMENT_OTLP_TRACING_ENDPOINT is correct"
fi

echo ""
echo "🌐 Jaeger UI: ${JAEGER_URL}"
echo ""
