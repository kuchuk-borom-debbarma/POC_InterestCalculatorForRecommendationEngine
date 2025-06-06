<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ import="java.util.*" %>

<html lang="en">
<head>
    <title>User Topic Scoring Algorithm</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 20px;
        }

        .container {
            max-width: 1000px;
            margin: 0 auto;
        }

        .menu {
            display: flex;
            flex-wrap: wrap;
            gap: 10px;
            margin-bottom: 20px;
        }

        .menu-option {
            padding: 10px 15px;
            background: #4CAF50;
            color: white;
            text-decoration: none;
            border-radius: 4px;
            cursor: pointer;
            border: none;
        }

        .menu-option:hover {
            background: #45a049;
        }

        .analytics-button {
            padding: 10px 15px;
            background: #2196F3;
            color: white;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            margin-top: 20px;
        }

        .analytics-button:hover {
            background: #0b7dda;
        }

        .hidden {
            display: none;
        }
    </style>
</head>
<body>
<div class="container">
    <h1>User Topic Scoring Algorithm</h1>

    <div class="menu">
        <button class="menu-option">1. Add content manually</button>
        <button class="menu-option">2. Add content using AI</button>
        <button class="menu-option">3. Simulate User interaction manually</button>
        <button class="menu-option">4. Simulate User interaction using AI</button>
    </div>

    <button id="showAnalytics" class="analytics-button">View Analytics</button>

    <div id="analyticsSection" class="hidden">
        <h2>Topic Analytics Preview</h2>


    </div>
</div>

<script>
    document.getElementById('showAnalytics').addEventListener('click', function () {
        // In a real application, this would navigate to a new page
        // window.location.href = '/analytics';

        // For now, just show/hide the preview section
        const analyticsSection = document.getElementById('analyticsSection');
        analyticsSection.classList.toggle('hidden');

        // Change button text based on state
        this.textContent = analyticsSection.classList.contains('hidden')
            ? 'View Analytics'
            : 'Hide Analytics Preview';
    });
</script>
</body>
</html>