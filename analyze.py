from parse_output import get_data
from graphers import graph_performance


def main():
    filename = 'console'
    data = get_data(filename)
    graph_performance(data)


if __name__ == '__main__':
    main()