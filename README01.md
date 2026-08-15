# 🚀 Team Development Workflow Guide

Welcome to the team! To maintain high code quality and keep our application stable, our organization enforces a strict **Feature Branch Workflow** combined with **Branch Protection Rules** on the `main` branch. 

Please read and follow these steps for every code contribution.

---

## 🔒 Branch Rules Summary
* **Direct pushes to `main` are blocked.** All changes must go through a Pull Request (PR).
* **Code Reviews are mandatory.** Every PR requires at least **1 approval** from another developer before it can be merged.

---

## 🛠️ The 4-Step Workflow

### Step 1: Clone the Repository (First time only)
If you haven't already, clone the repository to your local machine and navigate into the project directory:

```bash
git clone https://github.com
cd YOUR-REPO-NAME
```
*(Make sure to replace `YOUR-ORGANIZATION-NAME` and `YOUR-REPO-NAME` with our actual GitHub paths.)*

---

### Step 2: Create a Feature Branch
Never write code directly on the `main` branch. Always ensure your local `main` branch is up to date, then create a new branch named after the feature or bug you are working on:

```bash
# Switch to main and pull the latest changes
git checkout main
git pull origin main

# Create and switch to your new feature branch
git checkout -b feature/short-description
```
*Example branch names: `feature/user-login`, `bugfix/fix-header-spacing`*

---

### Step 3: Code, Stage, and Commit
Write your code locally. Once your changes are working and tested, stage your files and commit them with a clear, descriptive message:

```bash
# Check which files you changed
git status

# Stage all your changes
git add .

# Commit your changes with a meaningful message
git commit -m "feat: implement user login validation logic"
```

---

### Step 4: Push to GitHub & Open a Pull Request (PR)
Push your local branch to the remote GitHub organization server:

```bash
git push origin feature/short-description
```

Once pushed, follow these steps on GitHub:
1. Open our organization's repository page on GitHub.
2. Click the green **"Compare & pull request"** button that automatically appears at the top.
3. Write a brief description of what your code does or what issue it solves.
4. Assign at least **one peer developer** from the team as a **Reviewer**.
5. Once they approve your changes and the automated status checks pass, click **"Merge pull request"** and safely delete your feature branch.

---

## 💡 Best Practices
* **Keep PRs small:** Small pull requests are easier to review, get approved faster, and introduce fewer bugs.
* **Pull frequently:** Run `git pull origin main` often to minimize complex merge conflicts later.
* **Write meaningful commit messages:** Use prefixes like `feat:` (new feature), `fix:` (bug fix), or `docs:` (documentation changes).
