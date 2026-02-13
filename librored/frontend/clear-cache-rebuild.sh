#!/bin/bash

# LibroRed Cache Clear and Rebuild Script
# This script helps with caching issues by doing a complete clean rebuild

echo "Starting LibroRed cache clear and rebuild process..."
echo ""

# Step 1: Kill all running processes
echo "Step 1: Killing all running processes..."
echo "  - Stopping npm start processes..."
pkill -f "npm start" || true

echo "  - Stopping mvn spring-boot:run processes..."
pkill -f "mvn spring-boot:run" || true

echo "  - Killing processes on port 8443 (backend)..."
lsof -ti:8443 | xargs -r kill -9 2>/dev/null || true

echo "  - Killing processes on port 4200 (frontend dev)..."
lsof -ti:4200 | xargs -r kill -9 2>/dev/null || true

echo "All processes stopped"
echo ""

# Step 2: Clear all build caches completely
echo "⏺ Step 2: Clearing all build caches..."
echo "  - Removing Angular dist and cache directories..."
rm -rf dist/ .angular/cache/

echo "  - Removing node_modules cache..."
rm -rf node_modules/.cache/

echo "  - Removing backend static files..."
rm -rf ../backend/src/main/resources/static/new/

echo "All caches cleared"
echo ""

# Step 3: Force fresh build
echo "⏺ Step 3: Building fresh Angular application..."
npm run build

if [ $? -eq 0 ]; then
    # Check build timestamp
    if [ -f "../backend/src/main/resources/static/new/main.js" ]; then
        BUILD_TIME=$(stat -f "%Sm" -t "%H:%M:%S" "../backend/src/main/resources/static/new/main.js")
        echo "✅ Fresh build completed at $BUILD_TIME"
        ls -la ../backend/src/main/resources/static/new/main.js | awk '{print "   File size:", $5, "bytes, modified:", $6, $7, $8}'
    else
        echo "❌ Build files not found in expected location"
        exit 1
    fi
else
    echo "❌ Build failed"
    exit 1
fi
echo ""

# Step 4: Start backend
echo "Step 4: Starting backend with fresh build..."
echo "  - Starting Spring Boot application..."
echo "  - Backend will be available at https://localhost:8443/"
echo "  - Angular SPA will be available at https://localhost:8443/new/"
echo ""
echo "🎯 To start backend, run:"
echo "   cd ../backend && mvn spring-boot:run"
echo ""
echo "🎉 Cache clear and rebuild process completed successfully!"
echo "   You can now test the application without any cached elements."