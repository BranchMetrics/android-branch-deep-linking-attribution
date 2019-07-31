# String formatters
MAGENTA = '\033[95m'
BLUE = '\033[94m'
DULLGREEN = '\033[32m'
GREEN = '\033[92m'
DULLYELLOW = '\033[33m'
YELLOW = '\033[93m'
DULLRED = '\033[31m'
RED = '\033[91m'
RESET = '\033[0m'
BOLD = '\033[1m'
UNDERLINE = '\033[4m'


def in_bold(str_msg):
    formatted_str_msg = BOLD + str_msg + RESET
    return formatted_str_msg


def in_bold_yellow(str_msg):
    formatted_str_msg = DULLYELLOW + BOLD + str_msg + RESET
    return formatted_str_msg


def in_bold_green(str_msg):
    formatted_str_msg = DULLGREEN + BOLD + str_msg + RESET
    return formatted_str_msg


def in_bold_red(str_msg):
    formatted_str_msg = DULLRED + BOLD + str_msg + RESET
    return formatted_str_msg
