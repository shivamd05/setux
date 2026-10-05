# Git and GitHub workflow for SetuX

This guide assumes the whole SetuX folder will be one repository containing all three services. That is the simplest layout for this project while you are learning: a change that affects the gateway and analytics service can be reviewed together.

## First, verify the repository boundary

Run these commands from the SetuX folder:

```bash
pwd
git rev-parse --show-toplevel
git remote -v
```

The Git root should be the SetuX folder. In the current workspace, Git instead reports `/Users/shivamdate` and a `Spring_Learning.git` remote. That repository includes unrelated files. Do not stage, commit, or push from that parent root for SetuX.

Create a new empty GitHub repository for SetuX (do not initialize it with a README if you already have this project README). Then, after confirming the Git root is SetuX, connect it and publish the initial branch:

```bash
git remote add origin https://github.com/YOUR_USERNAME/SetuX.git
git branch -M main
git add .
git status
git commit -m "chore: add SetuX service foundations"
git push -u origin main
```

Replace `YOUR_USERNAME` with your GitHub username. Review `git status` before every commit so you know exactly which files are included. If `git rev-parse --show-toplevel` still shows `/Users/shivamdate`, stop and correct the repository setup before running these commands.

## Everyday feature workflow

Start each feature on a short-lived branch:

```bash
git switch main
git pull --ff-only
git switch -c feat/route-management
```

Make a small change, inspect it, and commit it:

```bash
git status
git diff
git add api-management-service
git diff --cached
git commit -m "feat: add route management endpoint"
git push -u origin feat/route-management
```

Open a Pull Request on GitHub. Explain what changed and how you checked it. Review the diff, merge the PR when it is ready, then update your local main branch:

```bash
git switch main
git pull --ff-only
```

Use focused commit messages such as `feat: add route CRUD`, `fix: reject unknown routes`, `docs: explain local setup`, and `chore: update Maven wrapper`.

## Before committing

- Check `git status` and `git diff`.
- Keep passwords, tokens, private keys, and production connection strings out of Git. Put local secrets in environment variables or an ignored local configuration file, and commit a safe example such as `.env.example` instead.
- Do not commit build output such as Maven `target/`, IDE settings, or machine-specific files.
- Prefer small changes that do one thing, and explain unfamiliar changes in the Pull Request.

## Useful commands

```bash
git status                 # show changed and untracked files
git diff                   # inspect unstaged edits
git diff --cached          # inspect staged edits
git log --oneline -5       # view recent commits
git branch                 # list local branches
git switch main            # return to main
```

Git is the version control tool that records changes on your computer. GitHub hosts a copy of the repository and adds Pull Requests, review, and issue tracking. A commit saves a snapshot locally; `git push` sends commits to GitHub. A branch lets you work on a feature without changing `main` until it is reviewed and merged.
