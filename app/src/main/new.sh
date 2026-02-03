git filter-branch --force --index-filter \
      "git rm -rf --cached --ignore-unmatch app/src/main/assets/vits-persian" \
      --prune-empty --tag-name-filter cat -- --allrm -rf .git/refs/original/
    git reflog expire --expire=now --all
    git gc --prune=now --aggressive    du -sh .git
    