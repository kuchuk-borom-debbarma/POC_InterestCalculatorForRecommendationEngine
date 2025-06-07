@import dev.kuku.interestcalculator.fakeDatabase.ContentDb
@import java.util.List
@import java.time.Instant
@import java.time.ZoneId
@import java.time.format.DateTimeFormatter
@param List<ContentDb.ContentRow> contents

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Content Feed</title>
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css" rel="stylesheet">
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background-color: #f5f5f5;
            line-height: 1.6;
            display: flex;
        }

        .sidebar {
            width: 300px;
            background: white;
            box-shadow: 2px 0 10px rgba(0,0,0,0.1);
            padding: 20px;
            position: fixed;
            height: 100vh;
            overflow-y: auto;
            z-index: 1000;
        }

        .sidebar h3 {
            color: #333;
            margin-bottom: 20px;
            font-size: 1.2em;
            border-bottom: 2px solid #667eea;
            padding-bottom: 10px;
        }

        .control-group {
            margin-bottom: 25px;
        }

        .control-group label {
            display: block;
            font-weight: 600;
            color: #555;
            margin-bottom: 8px;
            font-size: 0.9em;
        }

        .control-group select {
            width: 100%;
            padding: 10px;
            border: 2px solid #e1e5e9;
            border-radius: 8px;
            font-size: 0.9em;
            background: white;
            transition: border-color 0.2s ease;
        }

        .control-group select:focus {
            outline: none;
            border-color: #667eea;
        }

        .user-id-input {
            width: 100%;
            padding: 10px;
            border: 2px solid #e1e5e9;
            border-radius: 8px;
            font-size: 0.9em;
            transition: border-color 0.2s ease;
        }

        .user-id-input:focus {
            outline: none;
            border-color: #667eea;
        }

        .current-settings {
            background: #f8f9fa;
            padding: 15px;
            border-radius: 8px;
            margin-top: 20px;
        }

        .current-settings h4 {
            color: #333;
            margin-bottom: 10px;
            font-size: 1em;
        }

        .setting-item {
            display: flex;
            justify-content: space-between;
            margin-bottom: 8px;
            font-size: 0.85em;
        }

        .setting-label {
            color: #666;
        }

        .setting-value {
            font-weight: 600;
            color: #333;
        }

        .main-content {
            margin-left: 320px;
            flex: 1;
            padding: 20px;
        }

        .container {
            max-width: 800px;
            margin: 0 auto;
        }

        .header {
            text-align: center;
            margin-bottom: 30px;
            color: #333;
        }

        .content-card {
            background: white;
            border-radius: 12px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            margin-bottom: 20px;
            padding: 20px;
            transition: transform 0.2s ease, box-shadow 0.2s ease;
        }

        .content-card:hover {
            transform: translateY(-2px);
            box-shadow: 0 4px 20px rgba(0,0,0,0.15);
        }

        .content-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 15px;
            padding-bottom: 10px;
            border-bottom: 1px solid #eee;
        }

        .content-id {
            font-size: 0.9em;
            color: #666;
            font-weight: 500;
        }

        .timestamp {
            font-size: 0.8em;
            color: #999;
        }

        .content-body {
            margin-bottom: 15px;
            color: #333;
            font-size: 1.1em;
            line-height: 1.7;
        }

        .topics {
            margin-bottom: 20px;
        }

        .topic-tag {
            display: inline-block;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 4px 12px;
            border-radius: 20px;
            font-size: 0.8em;
            margin-right: 8px;
            margin-bottom: 5px;
            font-weight: 500;
        }

        .user-info {
            font-size: 0.9em;
            color: #666;
            margin-bottom: 20px;
            display: flex;
            align-items: center;
        }

        .user-avatar {
            width: 24px;
            height: 24px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            border-radius: 50%;
            margin-right: 8px;
            display: flex;
            align-items: center;
            justify-content: center;
            color: white;
            font-size: 0.8em;
            font-weight: bold;
        }

        .actions {
            display: flex;
            gap: 15px;
            padding-top: 15px;
            border-top: 1px solid #eee;
        }

        .action-btn {
            display: flex;
            align-items: center;
            gap: 8px;
            padding: 8px 16px;
            border: none;
            border-radius: 25px;
            cursor: pointer;
            font-size: 0.9em;
            font-weight: 500;
            transition: all 0.2s ease;
            text-decoration: none;
            color: #666;
            background: #f8f9fa;
        }

        .action-btn:hover {
            transform: translateY(-1px);
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
        }

        .like-btn {
            color: #28a745;
            border: 1px solid #28a745;
        }

        .like-btn:hover {
            background: #28a745;
            color: white;
        }

        .dislike-btn {
            color: #dc3545;
            border: 1px solid #dc3545;
        }

        .dislike-btn:hover {
            background: #dc3545;
            color: white;
        }

        .comment-btn {
            color: #007bff;
            border: 1px solid #007bff;
        }

        .comment-btn:hover {
            background: #007bff;
            color: white;
        }

        .report-btn {
            color: #ffc107;
            border: 1px solid #ffc107;
        }

        .report-btn:hover {
            background: #ffc107;
            color: #333;
        }

        .empty-state {
            text-align: center;
            padding: 60px 20px;
            color: #666;
        }

        .empty-state i {
            font-size: 3em;
            margin-bottom: 20px;
            color: #ddd;
        }

        .success-message {
            background: #d4edda;
            color: #155724;
            padding: 10px 15px;
            border-radius: 8px;
            margin-bottom: 20px;
            border: 1px solid #c3e6cb;
            display: none;
        }

        .error-message {
            background: #f8d7da;
            color: #721c24;
            padding: 10px 15px;
            border-radius: 8px;
            margin-bottom: 20px;
            border: 1px solid #f5c6cb;
            display: none;
        }

        @media (max-width: 1024px) {
            .sidebar {
                width: 250px;
            }

            .main-content {
                margin-left: 270px;
            }
        }

        @media (max-width: 768px) {
            .sidebar {
                transform: translateX(-100%);
                transition: transform 0.3s ease;
            }

            .sidebar.open {
                transform: translateX(0);
            }

            .main-content {
                margin-left: 0;
            }

            .mobile-menu-btn {
                position: fixed;
                top: 20px;
                left: 20px;
                z-index: 1001;
                background: #667eea;
                color: white;
                border: none;
                padding: 10px;
                border-radius: 50%;
                cursor: pointer;
                display: block;
            }
        }

        .mobile-menu-btn {
            display: none;
        }
    </style>
</head>
<body>
<button class="mobile-menu-btn" onclick="toggleSidebar()">
    <i class="fas fa-bars"></i>
</button>

<div class="sidebar" id="sidebar">
    <h3><i class="fas fa-cog"></i> Interaction Settings</h3>

    <div class="control-group">
        <label for="userId">User ID:</label>
        <input type="text" id="userId" class="user-id-input" value="123" placeholder="Enter your user ID">
    </div>

    <div class="control-group">
        <label for="discoveryMethod">Discovery Method:</label>
        <select id="discoveryMethod">
            <option value="TRENDING">Trending</option>
            <option value="RECOMMENDATION">Recommendation</option>
            <option value="SEARCH">Search</option>
        </select>
    </div>

    <div class="current-settings">
        <h4><i class="fas fa-info-circle"></i> Current Settings</h4>
        <div class="setting-item">
            <span class="setting-label">User ID:</span>
            <span class="setting-value" id="currentUserId">123</span>
        </div>
        <div class="setting-item">
            <span class="setting-label">Discovery:</span>
            <span class="setting-value" id="currentDiscovery">TRENDING</span>
        </div>
        <div class="setting-item">
            <span class="setting-label">Time:</span>
            <span class="setting-value" id="currentTime">Real-time</span>
        </div>
    </div>
</div>

<div class="main-content">
    <div class="container">
        <div class="header">
            <h1><i class="fas fa-newspaper"></i> Content Feed</h1>
            <p>Discover and interact with community content</p>
        </div>

        <div class="success-message" id="successMessage"></div>
        <div class="error-message" id="errorMessage"></div>

        @if(contents.isEmpty())
            <div class="empty-state">
                <i class="fas fa-inbox"></i>
                <h3>No content available</h3>
                <p>Be the first to share something interesting!</p>
            </div>
        @else
            @for(var content : contents)
                <div class="content-card">
                    <div class="content-header">
                        <span class="content-id">#${content.contentId()}</span>
                        <span class="timestamp">
                                ${DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm")
                                    .withZone(ZoneId.systemDefault())
                                    .format(Instant.ofEpochMilli(content.timestamp()))}
                            </span>
                    </div>

                    <div class="user-info">
                        <div class="user-avatar">
                            ${content.userId().substring(0, 1).toUpperCase()}
                        </div>
                        <span>Posted by <strong>${content.userId()}</strong></span>
                    </div>

                    <div class="content-body">
                        ${content.content()}
                    </div>

                    @if(!content.topics().isEmpty())
                        <div class="topics">
                            @for(var topic : content.topics())
                                <span class="topic-tag">#${topic}</span>
                            @endfor
                        </div>
                    @endif

                    <div class="actions">
                        <button type="button" class="action-btn like-btn" onclick="interact('${content.contentId()}', 'like')">
                            <i class="fas fa-thumbs-up"></i>
                            Like
                        </button>

                        <button type="button" class="action-btn dislike-btn" onclick="interact('${content.contentId()}', 'dislike')">
                            <i class="fas fa-thumbs-down"></i>
                            Dislike
                        </button>

                        <button type="button" class="action-btn comment-btn" onclick="interact('${content.contentId()}', 'comment')">
                            <i class="fas fa-comment"></i>
                            Comment
                        </button>

                        <button type="button" class="action-btn report-btn" onclick="confirmAndInteract('${content.contentId()}', 'report')">
                            <i class="fas fa-flag"></i>
                            Report
                        </button>
                    </div>
                </div>
            @endfor
        @endif
    </div>
</div>

<script>
    // Update current settings display
    function updateCurrentSettings() {
        const userId = document.getElementById('userId').value;
        const discovery = document.getElementById('discoveryMethod').value;

        document.getElementById('currentUserId').textContent = userId;
        document.getElementById('currentDiscovery').textContent = discovery;
    }

    // Initialize and add event listeners
    document.getElementById('userId').addEventListener('input', updateCurrentSettings);
    document.getElementById('discoveryMethod').addEventListener('change', updateCurrentSettings);

    // Mobile sidebar toggle
    function toggleSidebar() {
        const sidebar = document.getElementById('sidebar');
        sidebar.classList.toggle('open');
    }

    // Show message functions
    function showSuccessMessage(message) {
        const successEl = document.getElementById('successMessage');
        successEl.textContent = message;
        successEl.style.display = 'block';
        document.getElementById('errorMessage').style.display = 'none';
        setTimeout(() => {
            successEl.style.display = 'none';
        }, 3000);
    }

    function showErrorMessage(message) {
        const errorEl = document.getElementById('errorMessage');
        errorEl.textContent = message;
        errorEl.style.display = 'block';
        document.getElementById('successMessage').style.display = 'none';
        setTimeout(() => {
            errorEl.style.display = 'none';
        }, 5000);
    }

    // Main interaction function
    async function interact(contentId, interactionType) {
        const userId = document.getElementById('userId').value;
        const discoveryMethod = document.getElementById('discoveryMethod').value;

        if (!userId.trim()) {
            showErrorMessage('Please enter a valid User ID');
            return;
        }

        const button = event.target.closest('.action-btn');
        const originalContent = button.innerHTML;

        // Show loading state
        button.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Processing...';
        button.disabled = true;

        try {
            const url = '/content/' + contentId + '/' + interactionType + '?discoveryMethod=' + discoveryMethod + '&userId=' + encodeURIComponent(userId);
            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                }
            });

            const responseText = await response.text();

            if (response.ok) {
                const capitalizedType = interactionType.charAt(0).toUpperCase() + interactionType.slice(1);
                showSuccessMessage(capitalizedType + ' recorded successfully!');
            } else {
                showErrorMessage('Error: ' + responseText);
            }
        } catch (error) {
            showErrorMessage('Network error. Please try again.');
            console.error('Interaction error:', error);
        } finally {
            // Restore button state
            button.innerHTML = originalContent;
            button.disabled = false;
        }
    }

    // Confirm and interact (for report button)
    function confirmAndInteract(contentId, interactionType) {
        if (confirm('Are you sure you want to report this content?')) {
            interact(contentId, interactionType);
        }
    }

    // Initialize current settings display
    updateCurrentSettings();

    // Add click feedback animation
    document.querySelectorAll('.action-btn').forEach(btn => {
        btn.addEventListener('mousedown', function() {
            this.style.transform = 'scale(0.95)';
        });

        btn.addEventListener('mouseup', function() {
            this.style.transform = '';
        });
    });
</script>
</body>
</html>