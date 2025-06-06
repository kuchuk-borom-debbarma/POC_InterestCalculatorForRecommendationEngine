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
        }

        .container {
            max-width: 800px;
            margin: 0 auto;
            padding: 20px;
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

        @media (max-width: 600px) {
            .container {
                padding: 10px;
            }

            .content-card {
                padding: 15px;
            }

            .actions {
                flex-wrap: wrap;
                gap: 10px;
            }

            .action-btn {
                font-size: 0.8em;
                padding: 6px 12px;
            }
        }
    </style>
</head>
<body>
<div class="container">
    <div class="header">
        <h1><i class="fas fa-newspaper"></i> Content Feed</h1>
        <p>Discover and interact with community content</p>
    </div>

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
                    <form action="/content/${content.contentId()}/like" method="post" style="display: inline;">
                        <button type="submit" class="action-btn like-btn">
                            <i class="fas fa-thumbs-up"></i>
                            Like
                        </button>
                    </form>

                    <form action="/content/${content.contentId()}/dislike" method="post" style="display: inline;">
                        <button type="submit" class="action-btn dislike-btn">
                            <i class="fas fa-thumbs-down"></i>
                            Dislike
                        </button>
                    </form>

                    <a href="/content/${content.contentId()}/comments" class="action-btn comment-btn">
                        <i class="fas fa-comment"></i>
                        Comment
                    </a>

                    <form action="/content/${content.contentId()}/report" method="post" style="display: inline;">
                        <button type="submit" class="action-btn report-btn"
                                onclick="return confirm('Are you sure you want to report this content?')">
                            <i class="fas fa-flag"></i>
                            Report
                        </button>
                    </form>
                </div>
            </div>
        @endfor
    @endif
</div>

<script>
    // Add some interactive feedback
    document.querySelectorAll('.action-btn').forEach(btn => {
        btn.addEventListener('click', function(e) {
            // Add a subtle animation feedback
            this.style.transform = 'scale(0.95)';
            setTimeout(() => {
                this.style.transform = '';
            }, 150);
        });
    });

    // Handle form submissions with loading states
    document.querySelectorAll('form').forEach(form => {
        form.addEventListener('submit', function(e) {
            const btn = this.querySelector('button');
            const originalText = btn.innerHTML;
            btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Processing...';
            btn.disabled = true;

            // Re-enable after 2 seconds (you might want to handle this differently)
            setTimeout(() => {
                btn.innerHTML = originalText;
                btn.disabled = false;
            }, 2000);
        });
    });
</script>
</body>
</html>