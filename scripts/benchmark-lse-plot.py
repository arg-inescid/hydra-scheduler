#!/usr/bin/env python3

from matplotlib import pyplot as plt
import numpy as np
import json

results_dir='/tmp/lse-results'

def numpy_ewma_vectorized(data, window):
    alpha = 2 /(window + 1.0)
    alpha_rev = 1-alpha

    scale = 1/alpha_rev
    n = data.shape[0]

    r = np.arange(n)
    scale_arr = scale**r
    offset = data[0]*alpha_rev**(r+1)
    pw0 = alpha*alpha_rev**(n-1)

    mult = data*pw0*scale_arr
    cumsums = mult.cumsum()
    out = offset + cumsums*scale_arr[::-1]
    return out

def plot_memory():

    def load_memory(filename):
        with open(filename) as metrics_file:
            dataset = json.load(metrics_file)
            dataset = dataset[10:] # OPTIONAL TO SKIP SOME INITIAL SETUP
            timestamps = [x["timestamp"] / 1000 / 60 for x in dataset]
            system_footprints = [x["system_footprint"] / 1000 for x in dataset]

            first_ts = timestamps[0]
            timestamps = [x - first_ts for x in timestamps]
            return [timestamps, system_footprints]

    # Loading memory and converting from MBs to GBs
    gh = load_memory(results_dir + '/gh_metrics.log')
    kn = load_memory(results_dir + '/kn_metrics.log')
    ow = load_memory(results_dir + '/ow_metrics.log')

    plt.rcParams.update({'font.size': 8})
    fig, ax = plt.subplots()

    ax.plot(ow[0], ow[1], linestyle = "-.", linewidth = 3, label = "OpenWhisk")
    ax.plot(kn[0], kn[1], linestyle = ":",  linewidth = 3, label = "Knative")
    ax.plot(gh[0], gh[1], linestyle = "-",  linewidth = 3, label = "GraalHost")
    ax.set_ylabel("System Footprint (GBs)")
    ax.set_xlabel("Time (min)")
    ax.set_ylim(ymin=0, ymax=100)
    ax.set_xlim(xmin=0, xmax=30)
    ax.grid()
    ax.legend()

    plt.tight_layout()
    plt.savefig("memory.pdf", bbox_inches="tight")
    plt.savefig("memory.png", dpi=300, bbox_inches="tight")

def plot_latency():

    def load_latency(filename):
        with open(filename) as manager_log_file:
            lines = manager_log_file.readlines()
            lines = [x for x in lines if "FINE Time" in x]
            latencies = [x.split("req: ", 1)[1].split(";", 1)[0] for x in lines]
            return [float(x) / 1000 for x in latencies]

    gh = load_latency(results_dir + '/gh_manager.log')
    kn = load_latency(results_dir + '/kn_manager.log')
    ow = load_latency(results_dir + '/ow_manager.log')

    x_gh = np.sort(gh)
    x_kn = np.sort(kn)
    x_ow = np.sort(ow)

    y_gh = np.arange(len(gh)) / float(len(gh))
    y_kn = np.arange(len(kn)) / float(len(kn))
    y_ow = np.arange(len(ow)) / float(len(ow))

    plt.rcParams.update({'font.size': 8})
    fig, ax = plt.subplots()

    ax.plot(x_ow, y_ow, linestyle = "-.",linewidth = 3, label = "OpenWhisk")
    ax.plot(x_kn, y_kn, linestyle = ":", linewidth = 3, label = "Knative")
    ax.plot(x_gh, y_gh, linestyle = "-", linewidth = 3, label = "GraalHost")
    ax.set_ylabel("CDF")
    ax.set_xlabel("Request Latency (ms)")
    ax.set_xscale('log')
    ax.set_ylim(ymin=0, ymax=1)
    ax.set_xlim(xmin=0)#, xmax=80000)
    ax.grid()
    ax.legend()

    plt.tight_layout()
    plt.savefig("latency.pdf", bbox_inches="tight")
    plt.savefig("latency.png", dpi=300, bbox_inches="tight")

def plot_lambdas():

    def load_lambdas(filename):
        with open(filename) as metrics_file:
            dataset = json.load(metrics_file)

            # Find index of first non-zero active_lambdas
            start_index = next((i for i, x in enumerate(dataset) if x["active_lambdas"] > 0), 0)
            dataset = dataset[start_index:]

            timestamps = [x["timestamp"] / 1000 / 60 for x in dataset]
            active_lambdas = [min(x["active_lambdas"], 104) for x in dataset]

            first_ts = timestamps[0]
            timestamps = [x - first_ts for x in timestamps]
            return [timestamps, active_lambdas]

    gh = load_lambdas(results_dir + '/gh_metrics.log')
    kn = load_lambdas(results_dir + '/kn_metrics.log')
    ow = load_lambdas(results_dir + '/ow_metrics.log')

    gh[1] = numpy_ewma_vectorized(np.array(gh[1]), 100)
    kn[1] = numpy_ewma_vectorized(np.array(kn[1]), 100)
    ow[1] = numpy_ewma_vectorized(np.array(ow[1]), 100)

    plt.rcParams.update({'font.size': 8})
    fig, ax = plt.subplots()

    ax.plot(ow[0], ow[1], linestyle = "-.",linewidth = 3, label = "OpenWhisk")
    ax.plot(kn[0], kn[1], linestyle = ":", linewidth = 3, label = "Knative")
    ax.plot(gh[0], gh[1], linestyle = "-", linewidth = 3, label = "GraalHost")
    ax.set_ylabel("Number of Instances")
    ax.set_xlabel("Time (min)")
    ax.set_ylim(ymin=0, ymax=120)
    ax.set_xlim(xmin=0, xmax=30)
    ax.grid()
    ax.legend()

    plt.tight_layout()
    plt.savefig("lambdas.pdf", bbox_inches="tight")
    plt.savefig("lambdas.png", dpi=300, bbox_inches="tight")

def plot_requests():

    def load_requests(filename):
        with open(filename) as metrics_file:
            dataset = json.load(metrics_file)

            # Find index of first non-zero active_lambdas
            start_index = next((i for i, x in enumerate(dataset) if x["open_requests"] > 0), 0)
            dataset = dataset[start_index:]

            timestamps = [x["timestamp"] / 1000 / 60 for x in dataset]
            open_requests = [min(x["open_requests"], 104) for x in dataset]

            first_ts = timestamps[0]
            timestamps = [x - first_ts for x in timestamps]
            return [timestamps, open_requests]

    gh = load_requests(results_dir + '/gh_metrics.log')
    kn = load_requests(results_dir + '/kn_metrics.log')
    ow = load_requests(results_dir + '/ow_metrics.log')

    gh[1] = numpy_ewma_vectorized(np.array(gh[1]), 100)
    kn[1] = numpy_ewma_vectorized(np.array(kn[1]), 100)
    ow[1] = numpy_ewma_vectorized(np.array(ow[1]), 100)

    plt.rcParams.update({'font.size': 8})
    fig, ax = plt.subplots()

    ax.plot(ow[0], ow[1], linestyle = "-.",linewidth = 3, label = "OpenWhisk")
    ax.plot(kn[0], kn[1], linestyle = ":", linewidth = 3, label = "Knative")
    ax.plot(gh[0], gh[1], linestyle = "-", linewidth = 3, label = "GraalHost")
    ax.set_ylabel("Active Requests")
    ax.set_xlabel("Time (min)")
    ax.set_ylim(ymin=0, ymax=120)
    ax.set_xlim(xmin=0, xmax=30)
    ax.grid()
    ax.legend()

    plt.tight_layout()
    plt.savefig("requests.pdf", bbox_inches="tight")
    plt.savefig("requests.png", dpi=300, bbox_inches="tight")

plot_memory()
plot_latency()
plot_lambdas()
plot_requests()