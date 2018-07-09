//
//  Ring.m
//  Ring
//
//  Created by chris on 7/5/18.
//  Copyright © 2018 Warebots, LLC. All rights reserved.
//

#import "WBRing.h"

@implementation WBRing
RCT_EXPORT_MODULE(Ring)

- (NSArray<NSString *> *)supportedEvents {
  return @[];
}

RCT_REMAP_METHOD(setup, setupWithPromise:(NSString *)appGroupName resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
  WBNode *node = [[WBNode alloc] initWithAppGroupName:appGroupName andQueue: [self methodQueue]];
  [self setNode:node];
  [node assertFilesExist];
  [self.node readData:^(BOOL success, NSDictionary *output) {
    resolve(output);
  }];
}

RCT_REMAP_METHOD(read, readWithResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
  [self.node readData:^(BOOL success, NSDictionary *output) {
    resolve(output);
  }];
}

RCT_REMAP_METHOD(write, writeWithVersion:(nonnull NSNumber *)version andData:(NSString *)data resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
  [self.node writeData:[version intValue] data:data withCompletion:^(BOOL success, NSDictionary *output) {
    resolve(output);
  }];
}

- (dispatch_queue_t)methodQueue {
  if (self.queue) {
    return self.queue;
  }
  self.queue = dispatch_queue_create("com.warebots.reactnative.ring.SharedLocalStorageQueue", DISPATCH_QUEUE_SERIAL);
  return self.queue;
}
@end
